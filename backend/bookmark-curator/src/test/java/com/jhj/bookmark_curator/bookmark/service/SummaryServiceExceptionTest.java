package com.jhj.bookmark_curator.bookmark.service;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.exception.ErrorCode;
import com.jhj.bookmark_curator.bookmark.exception.SummaryGenerationException;

import tools.jackson.databind.ObjectMapper;

class SummaryServiceExceptionTest {

    private SummaryService summaryService;
    private MockRestServiceServer mockServer;
    private final String apiUrl = "https://api.groq.com/openai/v1/chat/completions";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        summaryService = new SummaryService(new ObjectMapper(), restClient);
        ReflectionTestUtils.setField(summaryService, "apiUrl", apiUrl);
        ReflectionTestUtils.setField(summaryService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(summaryService, "model", "test-model");
        ReflectionTestUtils.setField(summaryService, "visionModel", "llama-3.2-11b-vision-preview");
    }

    @Test
    @DisplayName("Groq API가 429 Too Many Requests를 반환하면 SUMMARY_RATE_LIMITED 예외를 던진다")
    void throwRateLimitedWhenGroqReturns429() {
        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Rate limit reached\"}}"));

        assertThatThrownBy(() -> summaryService.summarize(ContentType.ARTICLE, "테스트 본문 내용"))
                .isInstanceOf(SummaryGenerationException.class)
                .satisfies(e -> {
                    SummaryGenerationException ex = (SummaryGenerationException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_RATE_LIMITED);
                    assertThat(ex.getMessage()).isEqualTo("Groq 요청 한도를 초과했습니다");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("Groq API 응답이 504 Gateway Timeout이면 SUMMARY_TIMEOUT 예외를 던진다")
    void throwTimeoutWhenGroqReturns504() {
        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        assertThatThrownBy(() -> summaryService.summarize(ContentType.ARTICLE, "테스트 본문 내용"))
                .isInstanceOf(SummaryGenerationException.class)
                .satisfies(e -> {
                    SummaryGenerationException ex = (SummaryGenerationException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_TIMEOUT);
                    assertThat(ex.getMessage()).isEqualTo("요약 생성 응답이 지연되고 있습니다");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("네트워크 연결 또는 응답 타임아웃 발생 시 SUMMARY_TIMEOUT 예외를 던진다")
    void throwTimeoutWhenNetworkTimeoutOccurs() {
        RestClient customClient = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw new ResourceAccessException("Connection timed out", new SocketTimeoutException("Read timed out"));
                })
                .build();

        SummaryService serviceWithTimeout = new SummaryService(new ObjectMapper(), customClient);
        ReflectionTestUtils.setField(serviceWithTimeout, "apiUrl", apiUrl);
        ReflectionTestUtils.setField(serviceWithTimeout, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(serviceWithTimeout, "model", "test-model");

        assertThatThrownBy(() -> serviceWithTimeout.summarize(ContentType.ARTICLE, "테스트 본문 내용"))
                .isInstanceOf(SummaryGenerationException.class)
                .satisfies(e -> {
                    SummaryGenerationException ex = (SummaryGenerationException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_TIMEOUT);
                });
    }

    @Test
    @DisplayName("Groq API가 500 등 그 외 에러를 반환하면 SUMMARY_GENERATION_FAILED 예외를 던진다")
    void throwGenerationFailedWhenGroqReturns500() {
        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> summaryService.summarize(ContentType.ARTICLE, "테스트 본문 내용"))
                .isInstanceOf(SummaryGenerationException.class)
                .satisfies(e -> {
                    SummaryGenerationException ex = (SummaryGenerationException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_GENERATION_FAILED);
                    assertThat(ex.getMessage()).isEqualTo("요약 생성에 실패했습니다");
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("Groq API 응답 JSON 파싱 실패 시 SUMMARY_GENERATION_FAILED 예외를 던진다")
    void throwGenerationFailedWhenJsonParsingFails() {
        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"invalid\": \"json response\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> summaryService.summarize(ContentType.ARTICLE, "테스트 본문 내용"))
                .isInstanceOf(SummaryGenerationException.class)
                .satisfies(e -> {
                    SummaryGenerationException ex = (SummaryGenerationException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_GENERATION_FAILED);
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("content가 비어있거나 공백일 때 Groq API를 호출하지 않고 고정된 폴백 결과를 반환한다 (IMAGE, DOCUMENT, OTHER)")
    void returnFallbackWithoutCallingGroqWhenContentIsBlank() {
        // IMAGE
        SummaryResult imageResult = summaryService.summarize(ContentType.IMAGE, "");
        assertThat(imageResult.summary()).isEqualTo("이미지 파일입니다. 메모를 직접 입력해주세요");
        assertThat(imageResult.tags()).containsExactly("이미지");

        // DOCUMENT
        SummaryResult docResult = summaryService.summarize(ContentType.DOCUMENT, "   ");
        assertThat(docResult.summary()).isEqualTo("요약할 수 있는 텍스트를 가져오지 못했습니다. 메모를 직접 입력해주세요");
        assertThat(docResult.tags()).containsExactly("문서");

        // OTHER (or null content)
        SummaryResult otherResult = summaryService.summarize(ContentType.OTHER, null);
        assertThat(otherResult.summary()).isEqualTo("요약할 수 있는 텍스트를 가져오지 못했습니다. 메모를 직접 입력해주세요");
        assertThat(otherResult.tags()).containsExactly("기타");

        // Groq API가 한 번도 호출되지 않았음을 검증
        mockServer.verify();
    }

    @Test
    @DisplayName("IMAGE 타입 및 URL 제공 시 비전 모델을 호출하여 요약과 태그를 정상 생성한다")
    void summarizeImageWithVisionSuccess() {
        String imageUrl = "https://example.com/sample.png";
        String mockVisionResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summary\\": \\"샘플 이미지 요약입니다.\\", \\"tags\\": [\\"샘플\\", \\"이미지\\"]}"
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockVisionResponse, MediaType.APPLICATION_JSON));

        SummaryResult result = summaryService.summarize(ContentType.IMAGE, "", imageUrl);

        assertThat(result.summary()).isEqualTo("샘플 이미지 요약입니다.");
        assertThat(result.tags()).containsExactly("샘플", "이미지");
        mockServer.verify();
    }

    @Test
    @DisplayName("비전 모델 호출 시 HTTP 에러(403 등)가 발생하면 예외를 던지지 않고 고정 폴백으로 복구된다")
    void fallbackWhenVisionModelFailsWithHttpError() {
        String imageUrl = "https://example.com/blocked.png";

        mockServer.expect(requestTo(apiUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("403 Forbidden - Hotlinking blocked"));

        SummaryResult result = summaryService.summarize(ContentType.IMAGE, "", imageUrl);

        assertThat(result.summary()).isEqualTo("이미지 파일입니다. 메모를 직접 입력해주세요");
        assertThat(result.tags()).containsExactly("이미지");
        mockServer.verify();
    }
}

