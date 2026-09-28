package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SummaryServiceTest {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private ContentFetchService contentFetchService;

    @Test
    @Order(1)
    @DisplayName("1단계: 고정된 샘플 텍스트를 SummaryService에 넣어 요약과 태그가 정상 생성되는지 단독 검증")
    void summarizeFixedSampleTextTest() {
        // given
        String sampleText = """
                Spring Boot는 단독 실행 가능한 상용화 수준의 스프링 기반 애플리케이션을 손쉽게 만들 수 있도록 돕는 프레임워크입니다.
                내장 톰캣(Tomcat)을 지원하여 별도의 외장 WAS 설치 없이도 JAR 파일 하나로 어디서나 즉시 실행할 수 있습니다.
                또한 다양한 스타터(Starter) 의존성과 자동 설정(Auto Configuration)을 제공하여 복잡한 설정을 획기적으로 줄여줍니다.
                """;

        // when
        SummaryResult result = summaryService.summarize(ContentType.ARTICLE, sampleText);

        // then
        System.out.println("==================================================");
        System.out.println("[1단계: 단독 요약 테스트 (고정 샘플)]");
        System.out.println("- 요약 (summary):\n" + result.summary());
        System.out.println("- 태그 (tags): " + result.tags());
        System.out.println("==================================================\n");

        assertThat(result.summary()).isNotBlank();
        assertThat(result.summary()).doesNotContain(sampleText); // 원문 텍스트 그대로 반환하지 않음
        assertThat(result.tags()).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("2단계: ContentFetchService -> SummaryService 파이프라인 통합 테스트 (아티클/GitHub/YouTube)")
    void fetchAndSummarizePipelineTest() {
        String[] testUrls = {
                "https://spring.io/blog",                              // ARTICLE
                "https://github.com/spring-projects/spring-boot",       // GITHUB
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"           // VIDEO
        };

        for (String url : testUrls) {
            // 1. Fetch
            ContentFetchResult fetched = contentFetchService.fetch(url);

            // 2. Summarize
            SummaryResult summarized = summaryService.summarize(fetched.contentType(), fetched.content());

            // Output
            System.out.println("==================================================");
            System.out.println("[2단계 파이프라인 테스트: " + fetched.contentType() + "]");
            System.out.println("- URL: " + url);
            System.out.println("- 판별 타입: " + fetched.contentType());
            System.out.println("- 수집 제목: " + fetched.title());
            System.out.println("- LLM 3줄 요약:\n" + summarized.summary());
            System.out.println("- LLM 생성 태그: " + summarized.tags());
            System.out.println("==================================================\n");

            assertThat(summarized.summary()).isNotBlank();
            assertThat(summarized.tags()).isNotEmpty();
        }
    }

    @Test
    @Order(3)
    @DisplayName("3단계: 실제 이미지 URL로 비전 모델 호출 테스트 및 위키미디어 403 폴백 검증")
    void testRealImageVision() {
        // 1. 봇 차단 없는 공개 이미지 (GitHub Raw) -> 비전 모델 요약 성공
        String openImageUrl = "https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/java/java.png";
        SummaryResult visionResult = summaryService.summarize(ContentType.IMAGE, "", openImageUrl);
        System.out.println("=== 공개 이미지 비전 분석 결과 ===");
        System.out.println("Summary: " + visionResult.summary());
        System.out.println("Tags: " + visionResult.tags());
        assertThat(visionResult.summary()).doesNotContain("이미지 파일입니다. 메모를 직접 입력해주세요");
        assertThat(visionResult.tags()).isNotEmpty();

        // 2. 위키미디어 이미지 (서버단 403 차단) -> 안전하게 고정 메시지로 폴백
        String wikimediaUrl = "https://upload.wikimedia.org/wikipedia/commons/4/45/A_small_cup_of_coffee.JPG";
        SummaryResult wikimediaResult = summaryService.summarize(ContentType.IMAGE, "", wikimediaUrl);
        System.out.println("=== 위키미디어 403 폴백 결과 ===");
        System.out.println("Summary: " + wikimediaResult.summary());
        System.out.println("Tags: " + wikimediaResult.tags());
        assertThat(wikimediaResult.summary()).isEqualTo("이미지 파일입니다. 메모를 직접 입력해주세요");
    }
}
