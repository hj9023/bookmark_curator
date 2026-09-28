package com.jhj.bookmark_curator.bookmark.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.exception.ErrorCode;
import com.jhj.bookmark_curator.bookmark.exception.SummaryGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SummaryService {

    private static final String PROMPT_TEMPLATE = """
            당신은 웹 콘텐츠를 요약하고 핵심 태그를 추출하는 AI 어시스턴트입니다.
            제공된 콘텐츠 타입과 텍스트를 분석하여 2~3줄 요약과 핵심 태그(3~5개)를 생성해주세요.

            [콘텐츠 타입]
            {contentType}

            [수집된 텍스트]
            {수집된 텍스트}

            [요구사항]
            1. summary는 핵심 내용을 한국어로 2~3줄(문장)로 요약해주세요. 원문을 그대로 복사하지 말고 핵심을 간결히 요약하세요.
            2. tags는 콘텐츠를 대표하는 핵심 키워드 3~5개를 한국어 또는 널리 쓰이는 영문 기술용어로 추출해주세요.
            3. 반드시 아래의 JSON 형식으로만 응답해야 합니다. 마크다운 코드블록(```json 등)이나 기타 설명 문장 없이 순수 JSON 객체만 반환하세요:
            {
              "summary": "2~3줄 요약 내용",
              "tags": ["태그1", "태그2", "태그3"]
            }
            """;

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public SummaryService(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    // 테스트용 생성자
    SummaryService(ObjectMapper objectMapper, RestClient restClient) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    public SummaryResult summarize(ContentType contentType, String content) {
        if (content == null || content.isBlank()) {
            return createFallbackResult(contentType);
        }

        try {
            // README 원칙: {contentType}, {수집된 텍스트} 변수 치환
            String prompt = PROMPT_TEMPLATE
                    .replace("{contentType}", contentType != null ? contentType.name() : ContentType.OTHER.name())
                    .replace("{수집된 텍스트}", content);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.3
            );

            String responseBody = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseSummaryResult(responseBody);
        } catch (HttpStatusCodeException e) {
            log.error("Groq API HTTP error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode().value() == 429) {
                throw new SummaryGenerationException(ErrorCode.SUMMARY_RATE_LIMITED, e);
            }
            if (e.getStatusCode().value() == 504) {
                throw new SummaryGenerationException(ErrorCode.SUMMARY_TIMEOUT, e);
            }
            throw new SummaryGenerationException(ErrorCode.SUMMARY_GENERATION_FAILED, e);
        } catch (ResourceAccessException e) {
            log.error("Groq API 네트워크/접근 오류: {}", e.getMessage(), e);
            if (isTimeout(e)) {
                throw new SummaryGenerationException(ErrorCode.SUMMARY_TIMEOUT, e);
            }
            throw new SummaryGenerationException(ErrorCode.SUMMARY_GENERATION_FAILED, e);
        } catch (SummaryGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Groq API 요약 처리 실패: {}", e.getMessage(), e);
            if (isTimeout(e)) {
                throw new SummaryGenerationException(ErrorCode.SUMMARY_TIMEOUT, e);
            }
            throw new SummaryGenerationException(ErrorCode.SUMMARY_GENERATION_FAILED, e);
        }
    }

    private boolean isTimeout(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof java.net.SocketTimeoutException
                || t instanceof java.net.http.HttpTimeoutException
                || t instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        String msg = t.getMessage();
        if (msg != null && msg.toLowerCase().contains("timeout")) {
            return true;
        }
        return isTimeout(t.getCause());
    }

    private SummaryResult parseSummaryResult(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new IllegalStateException("Groq API 응답에 choices가 없습니다.");
        }

        String rawJson = choices.get(0).path("message").path("content").asText();
        rawJson = cleanJsonString(rawJson);

        SummaryJsonDto dto = objectMapper.readValue(rawJson, SummaryJsonDto.class);
        List<String> tags = dto.tags() != null ? dto.tags() : Collections.emptyList();
        return new SummaryResult(dto.summary(), tags);
    }

    private SummaryResult createFallbackResult(ContentType contentType) {
        if (contentType == ContentType.IMAGE) {
            return new SummaryResult("이미지 파일입니다. 메모를 직접 입력해주세요", List.of("이미지"));
        }
        if (contentType == ContentType.DOCUMENT) {
            return new SummaryResult("요약할 수 있는 텍스트를 가져오지 못했습니다. 메모를 직접 입력해주세요", List.of("문서"));
        }
        return new SummaryResult("요약할 수 있는 텍스트를 가져오지 못했습니다. 메모를 직접 입력해주세요", List.of("기타"));
    }

    private String cleanJsonString(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SummaryJsonDto(
            String summary,
            List<String> tags
    ) {}
}
