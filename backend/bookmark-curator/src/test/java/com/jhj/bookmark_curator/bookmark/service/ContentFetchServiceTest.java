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

    @Test
    @DisplayName("한글 퍼센트 인코딩된 URL 경로에서 슬러그 제목을 정상적으로 디코딩하고 추출한다")
    void testSlugExtraction() {
        String msnUrl = "https://www.msn.com/ko-kr/news/other/10%EB%AA%85-%EC%88%A8%EC%A7%80%EA%B3%A0-92%EB%AA%85-%EB%8B%A4%EC%B3%A4%EB%8B%A4-%EB%B6%88%EC%95%88%ED%95%9C-%ED%9C%B4%EA%B2%8C%EC%86%8C-%EC%95%88%EC%A0%84-sbs-8%EB%89%B4%EC%8A%A4/vi-AA2d0JoP";
        String slug = contentFetchService.extractSlugTitle(msnUrl);
        assertThat(slug).isEqualTo("10명 숨지고 92명 다쳤다 불안한 휴게소 안전 sbs 8뉴스");

        String velogUrl = "https://velog.io/@developer/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B6%80%ED%8A%B8-JPA-%EC%A0%95%EB%B3%B5";
        String velogSlug = contentFetchService.extractSlugTitle(velogUrl);
        assertThat(velogSlug).isEqualTo("스프링 부트 JPA 정복");
    }

    @Test
    @DisplayName("이미지 URL의 경우 확장자를 기반으로 IMAGE로 판별하고 파일명을 title로, 빈 문자열을 content로 반환한다")
    void testImageFetch() {
        String imageUrl = "https://example.com/assets/logo.png";
        ContentFetchResult result = contentFetchService.fetch(imageUrl);
        assertThat(result.contentType()).isEqualTo(ContentType.IMAGE);
        assertThat(result.title()).isEqualTo("logo.png");
        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("동영상 파일 URL의 경우 확장자를 기반으로 VIDEO로 판별하고 파일명을 title로 반환한다")
    void testVideoFileFetch() {
        String videoUrl = "https://example.com/videos/sample-tutorial.mp4";
        ContentFetchResult result = contentFetchService.fetch(videoUrl);
        assertThat(result.contentType()).isEqualTo(ContentType.VIDEO);
        assertThat(result.title()).isEqualTo("sample-tutorial.mp4");
        assertThat(result.content()).contains("sample-tutorial.mp4");
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
