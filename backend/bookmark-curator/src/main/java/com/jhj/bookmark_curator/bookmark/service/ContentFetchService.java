package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Paths;

@Slf4j
@Service
public class ContentFetchService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 5000;
    private static final int MAX_CONTENT_LENGTH = 5000;

    public ContentFetchResult fetch(String url) {
        if (url == null || url.isBlank()) {
            return ContentFetchResult.emptyOther();
        }

        try {
            String lowerUrl = url.toLowerCase().trim();

            if (isYoutubeUrl(lowerUrl)) {
                return fetchVideo(url);
            } else if (isGithubUrl(lowerUrl)) {
                return fetchGithub(url);
            } else if (isImageUrl(lowerUrl)) {
                return fetchImage(url);
            } else if (isDocumentUrl(lowerUrl)) {
                return fetchDocument(url, lowerUrl);
            } else if (isArticleUrl(lowerUrl)) {
                return fetchArticle(url);
            } else {
                return fetchOther(url);
            }
        } catch (Exception e) {
            log.warn("URL 콘텐츠 수집 실패 (url: {}): {}", url, e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    // --- 타입 판별 메서드 ---

    private boolean isYoutubeUrl(String lowerUrl) {
        return lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("vimeo.com");
    }

    private boolean isGithubUrl(String lowerUrl) {
        return lowerUrl.contains("github.com");
    }

    private boolean isImageUrl(String lowerUrl) {
        String path = lowerUrl.split("\\?")[0];
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg")
                || path.endsWith(".bmp");
    }

    private boolean isDocumentUrl(String lowerUrl) {
        String path = lowerUrl.split("\\?")[0];
        boolean isDocFile = path.endsWith(".pdf") || path.endsWith(".docx") || path.endsWith(".doc")
                || path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".pptx");
        boolean isDocPath = lowerUrl.contains("/docs/") || lowerUrl.contains("/documentation/")
                || lowerUrl.contains("/wiki/") || lowerUrl.contains("/guide/")
                || lowerUrl.contains("/manual/") || lowerUrl.contains("/reference/");
        return isDocFile || isDocPath;
    }

    private boolean isArticleUrl(String lowerUrl) {
        // 경로 힌트
        if (lowerUrl.contains("/blog") || lowerUrl.contains("/news")
                || lowerUrl.contains("/article") || lowerUrl.contains("/post")
                || lowerUrl.contains("/posts") || lowerUrl.contains("/story")
                || lowerUrl.contains("/entry")) {
            return true;
        }

        // 도메인 힌트 (대표 블로그/뉴스 플랫폼)
        if (lowerUrl.contains("medium.com") || lowerUrl.contains("velog.io")
                || lowerUrl.contains("tistory.com") || lowerUrl.contains("brunch.co.kr")
                || lowerUrl.contains("substack.com") || lowerUrl.contains("news.")) {
            return true;
        }

        // 날짜 경로 패턴 (/2024/03/ 등)
        return lowerUrl.matches(".*/20\\d{2}/\\d{1,2}/.*");
    }

    // --- 콘텐츠 수집 메서드 ---

    private ContentFetchResult fetchVideo(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String description = getMetaTag(doc, "og:description");
            if (description.isBlank()) {
                description = getMetaTag(doc, "description");
            }

            String content = ("제목: " + title + "\n설명: " + description).trim();
            return new ContentFetchResult(ContentType.VIDEO, title, content);
        } catch (Exception e) {
            log.warn("Video 정보 수집 실패: {}", e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchGithub(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);

            Element readme = doc.selectFirst("article.markdown-body, #readme");
            String content;
            if (readme != null) {
                content = readme.text();
            } else {
                String description = getMetaTag(doc, "og:description");
                content = !description.isBlank() ? description : doc.body().text();
            }

            return new ContentFetchResult(ContentType.GITHUB, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("GitHub 정보 수집 실패: {}", e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchImage(String url) {
        String fileName = extractFileName(url);
        String title = fileName.isBlank() ? "Image" : fileName;
        String content = "파일명: " + title;
        return new ContentFetchResult(ContentType.IMAGE, title, content);
    }

    private ContentFetchResult fetchDocument(String url, String lowerUrl) {
        String path = lowerUrl.split("\\?")[0];
        boolean isDocFile = path.endsWith(".pdf") || path.endsWith(".docx") || path.endsWith(".doc")
                || path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".pptx");

        if (isDocFile) {
            String fileName = extractFileName(url);
            String title = fileName.isBlank() ? "Document" : fileName;
            return new ContentFetchResult(ContentType.DOCUMENT, title, "문서 파일: " + title);
        }

        // 웹 기반 문서 (/docs/, /wiki/ 등)
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);
            return new ContentFetchResult(ContentType.DOCUMENT, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("Document 웹페이지 수집 실패: {}", e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchArticle(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);
            return new ContentFetchResult(ContentType.ARTICLE, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("Article 수집 실패: {}", e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchOther(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);
            return new ContentFetchResult(ContentType.OTHER, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("일반 웹페이지 수집 실패: {}", e.getMessage());
            return ContentFetchResult.emptyOther();
        }
    }

    // --- 공통 헬퍼 메서드 ---

    private Document connect(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .get();
    }

    private String extractTitle(Document doc) {
        String ogTitle = getMetaTag(doc, "og:title");
        if (!ogTitle.isBlank()) {
            return ogTitle;
        }
        return doc.title() != null ? doc.title().trim() : "";
    }

    private String extractBodyText(Document doc) {
        doc.select("script, style, nav, footer, header, noscript, iframe, aside").remove();
        Element bodyElement = doc.selectFirst("article, main, [role=main], .post-content, .entry-content");
        if (bodyElement != null) {
            return bodyElement.text();
        }
        return doc.body() != null ? doc.body().text() : "";
    }

    private String getMetaTag(Document doc, String attrName) {
        Element meta = doc.selectFirst("meta[property=" + attrName + "], meta[name=" + attrName + "]");
        if (meta != null) {
            String content = meta.attr("content");
            if (content != null) {
                return content.trim();
            }
        }
        return "";
    }

    private String extractFileName(String url) {
        try {
            String path = URI.create(url).getPath();
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
