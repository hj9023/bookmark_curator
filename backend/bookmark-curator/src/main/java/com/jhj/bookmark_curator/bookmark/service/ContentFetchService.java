package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        boolean isDocPath = lowerUrl.contains("/docs") || lowerUrl.contains("docs.")
                || lowerUrl.contains("/documentation") || lowerUrl.contains("/wiki")
                || lowerUrl.contains("/guide") || lowerUrl.contains("/manual")
                || lowerUrl.contains("/reference");
        return isDocFile || isDocPath;
    }

    private boolean isArticleUrl(String lowerUrl) {
        // 경로 힌트
        if (lowerUrl.contains("/blog") || lowerUrl.contains("/news")
                || lowerUrl.contains("/article") || lowerUrl.contains("/post")
                || lowerUrl.contains("/posts") || lowerUrl.contains("/story")
                || lowerUrl.contains("/entry") || lowerUrl.contains("/view/")
                || lowerUrl.contains("/detail/") || lowerUrl.contains("/read/")
                || lowerUrl.contains("/press/")) {
            return true;
        }

        // 도메인 힌트 (대표 블로그/뉴스 플랫폼 및 주요 언론사)
        if (lowerUrl.contains("medium.com") || lowerUrl.contains("velog.io")
                || lowerUrl.contains("tistory.com") || lowerUrl.contains("brunch.co.kr")
                || lowerUrl.contains("substack.com") || lowerUrl.contains("news.")
                || lowerUrl.contains("yna.co.kr") || lowerUrl.contains("chosun.com")
                || lowerUrl.contains("joongang.co.kr") || lowerUrl.contains("donga.com")
                || lowerUrl.contains("hani.co.kr") || lowerUrl.contains("khan.co.kr")
                || lowerUrl.contains("newsis.com") || lowerUrl.contains("news1.kr")
                || lowerUrl.contains("ytn.co.kr") || lowerUrl.contains("sbs.co.kr")
                || lowerUrl.contains("kbs.co.kr") || lowerUrl.contains("mbc.co.kr")
                || lowerUrl.contains("hankyung.com") || lowerUrl.contains("mk.co.kr")
                || lowerUrl.contains("zdnet.co.kr") || lowerUrl.contains("etnews.com")) {
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
        String title = fileName.isBlank() ? "" : fileName;
        return new ContentFetchResult(ContentType.IMAGE, title, "");
    }

    private ContentFetchResult fetchDocument(String url, String lowerUrl) {
        String path = lowerUrl.split("\\?")[0];
        boolean isDocFile = path.endsWith(".pdf") || path.endsWith(".docx") || path.endsWith(".doc")
                || path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".pptx");

        if (isDocFile) {
            String fileName = extractFileName(url);
            String title = fileName.isBlank() ? "" : fileName;
            return new ContentFetchResult(ContentType.DOCUMENT, title, "");
        }

        // 웹 기반 문서 (/docs/, /wiki/ 등)
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);

            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                if (title.isBlank() || isGenericTitle(title)) {
                    title = slugTitle;
                }
                if (content.isBlank()) {
                    content = "문서 제목: " + title;
                }
            }

            return new ContentFetchResult(ContentType.DOCUMENT, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("Document 웹페이지 수집 실패: {}", e.getMessage());
            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                return new ContentFetchResult(ContentType.DOCUMENT, slugTitle, "문서 제목: " + slugTitle);
            }
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchArticle(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);

            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                if (title.isBlank() || isGenericTitle(title)) {
                    title = slugTitle;
                }
                if (content.isBlank()) {
                    content = "제목: " + title;
                }
            }

            return new ContentFetchResult(ContentType.ARTICLE, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("Article 수집 실패: {}", e.getMessage());
            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                return new ContentFetchResult(ContentType.ARTICLE, slugTitle, "제목: " + slugTitle);
            }
            return ContentFetchResult.emptyOther();
        }
    }

    private ContentFetchResult fetchOther(String url) {
        try {
            Document doc = connect(url);
            String title = extractTitle(doc);
            String content = extractBodyText(doc);

            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                if (title.isBlank() || isGenericTitle(title)) {
                    title = slugTitle;
                }
                if (content.isBlank()) {
                    content = "제목: " + title;
                }
            }

            // HTML 메타태그(og:type 등)에서 article 여부 판별
            ContentType resolvedType = isArticlePage(doc, url) ? ContentType.ARTICLE : ContentType.OTHER;

            return new ContentFetchResult(resolvedType, title, truncate(content, MAX_CONTENT_LENGTH));
        } catch (Exception e) {
            log.warn("일반 웹페이지 수집 실패: {}", e.getMessage());
            String slugTitle = extractSlugTitle(url);
            if (!slugTitle.isBlank()) {
                return new ContentFetchResult(ContentType.OTHER, slugTitle, "제목: " + slugTitle);
            }
            return ContentFetchResult.emptyOther();
        }
    }

    private boolean isArticlePage(Document doc, String url) {
        if (doc == null) {
            return false;
        }
        if (url != null) {
            String lower = url.toLowerCase();
            if (lower.contains("/projects/") || lower.contains("/products/")) {
                return false;
            }
        }
        String ogType = getMetaTag(doc, "og:type");
        return ogType != null && ogType.toLowerCase().contains("article");
    }

    // --- 공통 헬퍼 메서드 ---

    public String extractSlugTitle(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String withoutProtocol = url.replaceFirst("^[a-zA-Z]+://", "");
            int firstSlash = withoutProtocol.indexOf('/');
            if (firstSlash < 0 || firstSlash == withoutProtocol.length() - 1) {
                return "";
            }

            String pathOnly = withoutProtocol.substring(firstSlash + 1).split("[?#]")[0];
            String[] segments = pathOnly.split("/");

            String bestSlug = "";
            for (String segment : segments) {
                if (segment.isBlank()) continue;
                String decoded;
                try {
                    decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    decoded = segment;
                }

                int dotIdx = decoded.lastIndexOf('.');
                if (dotIdx > 0 && dotIdx > decoded.length() - 6) {
                    decoded = decoded.substring(0, dotIdx);
                }

                boolean hasKorean = containsKorean(decoded);
                boolean isComposite = decoded.contains("-") || decoded.contains("_");

                if ((hasKorean || isComposite) && decoded.length() > bestSlug.length()) {
                    bestSlug = decoded;
                }
            }

            if (!bestSlug.isBlank()) {
                return bestSlug.replace("-", " ")
                        .replace("_", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        } catch (Exception e) {
            log.warn("URL 슬러그 추출 실패 (url: {}): {}", url, e.getMessage());
        }
        return "";
    }

    private boolean containsKorean(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                    || block == Character.UnicodeBlock.HANGUL_JAMO
                    || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }

    private boolean isGenericTitle(String title) {
        if (title == null || title.isBlank()) return true;
        String lower = title.trim().toLowerCase();
        return lower.equals("msn") || lower.startsWith("msn ") || lower.startsWith("msn -")
                || lower.equals("home") || lower.equals("naver") || lower.equals("daum")
                || lower.equals("untitled") || lower.equals("홈") || lower.equals("메인");
    }

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
            String cleanUrl = url.split("[?#]")[0];
            int lastSlash = cleanUrl.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < cleanUrl.length() - 1) {
                String fileName = cleanUrl.substring(lastSlash + 1);
                return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            }
            return "";
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
