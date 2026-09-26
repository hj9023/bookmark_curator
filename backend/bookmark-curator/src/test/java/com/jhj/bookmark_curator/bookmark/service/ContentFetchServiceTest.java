package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContentFetchServiceTest {

    @Autowired
    private ContentFetchService contentFetchService;

    @Test
    @DisplayName("URL 패턴 및 메타데이터를 기반으로 각 콘텐츠 타입을 일관성 있게 판별하고 수집한다")
    void fetchRealUrlsIntegrationTest() {
        // 1. Article URL 테스트 (/blog 경로) -> ARTICLE
        String articleUrl = "https://spring.io/blog";
        ContentFetchResult articleResult = contentFetchService.fetch(articleUrl);
        printResult("ARTICLE (블로그)", articleUrl, articleResult);
        assertThat(articleResult.contentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(articleResult.content()).isNotBlank();

        // 2. GitHub URL 테스트 -> GITHUB
        String githubUrl = "https://github.com/spring-projects/spring-boot";
        ContentFetchResult githubResult = contentFetchService.fetch(githubUrl);
        printResult("GITHUB", githubUrl, githubResult);
        assertThat(githubResult.contentType()).isEqualTo(ContentType.GITHUB);
        assertThat(githubResult.content()).isNotBlank();

        // 3. YouTube URL 테스트 -> VIDEO
        String youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        ContentFetchResult youtubeResult = contentFetchService.fetch(youtubeUrl);
        printResult("VIDEO", youtubeUrl, youtubeResult);
        assertThat(youtubeResult.contentType()).isEqualTo(ContentType.VIDEO);
        assertThat(youtubeResult.content()).isNotBlank();

        // 4. Document URL 테스트 (/docs/ 경로) -> DOCUMENT
        String docUrl = "https://docs.spring.io/spring-boot/index.html";
        ContentFetchResult docResult = contentFetchService.fetch(docUrl);
        printResult("DOCUMENT (공식 문서)", docUrl, docResult);
        assertThat(docResult.contentType()).isEqualTo(ContentType.DOCUMENT);
        assertThat(docResult.content()).isNotBlank();

        // 5. 일반 소개/랜딩 페이지 -> OTHER
        String landingUrl = "https://spring.io/projects/spring-boot";
        ContentFetchResult landingResult = contentFetchService.fetch(landingUrl);
        printResult("OTHER (일반 랜딩 페이지)", landingUrl, landingResult);
        assertThat(landingResult.contentType()).isEqualTo(ContentType.OTHER);
        assertThat(landingResult.content()).isNotBlank();

        // 6. fetch 실패 URL -> OTHER (빈 문자열)
        String invalidUrl = "https://this-is-an-invalid-nonexistent-domain-9999.org";
        ContentFetchResult invalidResult = contentFetchService.fetch(invalidUrl);
        printResult("OTHER (fetch 실패)", invalidUrl, invalidResult);
        assertThat(invalidResult.contentType()).isEqualTo(ContentType.OTHER);
        assertThat(invalidResult.content()).isEmpty();
    }

    private void printResult(String label, String url, ContentFetchResult result) {
        System.out.println("==================================================");
        System.out.println("[" + label + " 테스트]");
        System.out.println("URL: " + url);
        System.out.println("판별된 ContentType: " + result.contentType());
        System.out.println("제목: " + result.title());

        String snippet = result.content();
        if (snippet != null && snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "... (생략)";
        }
        System.out.println("수집된 텍스트 일부:\n" + snippet);
        System.out.println("==================================================\n");
    }
}
