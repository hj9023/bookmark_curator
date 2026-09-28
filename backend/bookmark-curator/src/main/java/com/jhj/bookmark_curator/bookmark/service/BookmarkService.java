package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.domain.Tag;
import com.jhj.bookmark_curator.bookmark.exception.BookmarkNotFoundException;
import com.jhj.bookmark_curator.bookmark.exception.InvalidUrlException;
import com.jhj.bookmark_curator.bookmark.repository.BookmarkRepository;
import com.jhj.bookmark_curator.bookmark.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;
    private final ContentFetchService contentFetchService;
    private final SummaryService summaryService;

    public Bookmark create(String url, String title, String memo) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL은 필수입니다.");
        }

        // 1. ContentFetchService로 콘텐츠 수집 및 contentType 판별
        ContentFetchResult fetched = contentFetchService.fetch(url);

        // 2. SummaryService로 요약/태그 생성
        SummaryResult summarized = summaryService.summarize(fetched.contentType(), fetched.content(), url);

        // 3. title 결정: 사용자 입력 우선, 없으면 fetch된 제목, 둘 다 없으면 URL
        String finalTitle = resolveTitle(title, fetched.title(), url);

        // 4. 태그 처리: 기존 태그 매칭 재사용, 없으면 신규 생성
        Set<Tag> tags = resolveTags(summarized.tags());

        // 5. Bookmark 엔티티 생성 및 저장
        Bookmark bookmark = Bookmark.builder()
                .url(url)
                .title(finalTitle)
                .memo(memo)
                .summary(summarized.summary())
                .contentType(fetched.contentType())
                .tags(tags)
                .build();

        return bookmarkRepository.save(bookmark);
    }

    @Transactional(readOnly = true)
    public List<Bookmark> findAll(ContentType contentType, String tag, String keyword) {
        String normalizedTag = (tag != null && !tag.isBlank()) ? Tag.normalize(tag) : null;
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return bookmarkRepository.search(contentType, normalizedTag, trimmedKeyword);
    }

    public Bookmark update(Long id, String title, String memo, ContentType contentType, List<String> tagNames) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new BookmarkNotFoundException(id));

        // URL과 summary는 수정 불가 (title, memo, contentType, tags 수정 가능)
        Set<Tag> updatedTags = tagNames != null ? resolveTags(tagNames) : null;
        bookmark.update(title, memo, contentType, updatedTags);

        return bookmark;
    }

    public void delete(Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new BookmarkNotFoundException(id));

        // 조인 테이블만 정리되고 연결된 Tag 엔티티 자체는 삭제되지 않음
        bookmarkRepository.delete(bookmark);
    }

    private String resolveTitle(String userTitle, String fetchedTitle, String url) {
        if (userTitle != null && !userTitle.isBlank()) {
            return userTitle.trim();
        }
        if (fetchedTitle != null && !fetchedTitle.isBlank()) {
            return fetchedTitle.trim();
        }
        return url;
    }

    private Set<Tag> resolveTags(Collection<String> tagNames) {
        Set<Tag> resolvedTags = new HashSet<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return resolvedTags;
        }

        for (String tagName : tagNames) {
            String normalized = Tag.normalize(tagName);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }

            Tag tag = tagRepository.findByName(normalized)
                    .orElseGet(() -> tagRepository.save(new Tag(normalized)));
            resolvedTags.add(tag);
        }
        return resolvedTags;
    }
}
