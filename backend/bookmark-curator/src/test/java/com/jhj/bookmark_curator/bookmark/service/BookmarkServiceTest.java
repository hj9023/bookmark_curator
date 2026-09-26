package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.domain.Tag;
import com.jhj.bookmark_curator.bookmark.repository.BookmarkRepository;
import com.jhj.bookmark_curator.bookmark.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ContentFetchService contentFetchService;

    @Mock
    private SummaryService summaryService;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    @DisplayName("create: 콘텐츠 수집 -> 요약/태깅 -> 태그 재사용/신규생성 -> 북마크 저장 검증")
    void createBookmarkTest() {
        // given
        String url = "https://example.com/article";
        String userMemo = "나중에 읽을 메모";

        // Mock 1. ContentFetchService 결과
        ContentFetchResult fetchResult = new ContentFetchResult(
                ContentType.ARTICLE,
                "수집된 아티클 제목",
                "수집된 본문 텍스트 내용..."
        );
        given(contentFetchService.fetch(url)).willReturn(fetchResult);

        // Mock 2. SummaryService 결과 (태그 2개: 기존 'ai', 신규 'spring')
        SummaryResult summaryResult = new SummaryResult(
                "LLM이 생성한 3줄 요약 내용입니다.",
                List.of("AI", "Spring")
        );
        given(summaryService.summarize(ContentType.ARTICLE, "수집된 본문 텍스트 내용..."))
                .willReturn(summaryResult);

        // Mock 3. TagRepository: 'ai'는 기존 태그 존재, 'spring'은 신규 저장
        Tag existingTag = new Tag("ai");
        Tag newTag = new Tag("spring");
        given(tagRepository.findByName("ai")).willReturn(Optional.of(existingTag));
        given(tagRepository.findByName("spring")).willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class))).willReturn(newTag);

        // Mock 4. BookmarkRepository 저장
        given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when (사용자 title 미입력 -> 수집된 제목 사용)
        Bookmark created = bookmarkService.create(url, null, userMemo);

        // then
        assertThat(created).isNotNull();
        assertThat(created.getUrl()).isEqualTo(url);
        assertThat(created.getTitle()).isEqualTo("수집된 아티클 제목");
        assertThat(created.getMemo()).isEqualTo(userMemo);
        assertThat(created.getSummary()).isEqualTo("LLM이 생성한 3줄 요약 내용입니다.");
        assertThat(created.getContentType()).isEqualTo(ContentType.ARTICLE);

        // 태그 검증: 'ai', 'spring' 2개 포함
        assertThat(created.getTags()).hasSize(2);
        assertThat(created.getTags())
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("ai", "spring");

        verify(contentFetchService).fetch(url);
        verify(summaryService).summarize(ContentType.ARTICLE, "수집된 본문 텍스트 내용...");
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("update: title, memo, contentType, tags가 정상 수정되고 url, summary는 유지된다")
    void updateBookmarkTest() {
        // given
        Long bookmarkId = 1L;
        Tag oldTag = new Tag("old-tag");
        Bookmark existingBookmark = Bookmark.builder()
                .url("https://example.com/original")
                .title("기존 제목")
                .memo("기존 메모")
                .summary("기존 요약 내용")
                .contentType(ContentType.ARTICLE)
                .tags(Set.of(oldTag))
                .build();

        given(bookmarkRepository.findById(bookmarkId)).willReturn(Optional.of(existingBookmark));

        // 새 태그 mock
        Tag newTag = new Tag("new-tag");
        given(tagRepository.findByName("new-tag")).willReturn(Optional.of(newTag));

        // when (contentType을 VIDEO로 수정)
        Bookmark updated = bookmarkService.update(bookmarkId, "수정된 제목", "수정된 메모", ContentType.VIDEO, List.of("new-tag"));

        // then: 수정 가능한 필드 확인
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getMemo()).isEqualTo("수정된 메모");
        assertThat(updated.getContentType()).isEqualTo(ContentType.VIDEO);
        assertThat(updated.getTags()).extracting(Tag::getName).containsExactly("new-tag");

        // 수정 불가 필드 유지 확인
        assertThat(updated.getUrl()).isEqualTo("https://example.com/original");
        assertThat(updated.getSummary()).isEqualTo("기존 요약 내용");
    }

    @Test
    @DisplayName("update: contentType이나 memo가 null이면 기존 값이 유지된다")
    void updateBookmarkKeepExistingValuesWhenNull() {
        // given
        Long bookmarkId = 1L;
        Bookmark existingBookmark = Bookmark.builder()
                .url("https://example.com/original")
                .title("기존 제목")
                .memo("기존 메모")
                .summary("기존 요약 내용")
                .contentType(ContentType.ARTICLE)
                .tags(Set.of(new Tag("old-tag")))
                .build();

        given(bookmarkRepository.findById(bookmarkId)).willReturn(Optional.of(existingBookmark));

        // when (contentType과 memo에 null 전달)
        Bookmark updated = bookmarkService.update(bookmarkId, "새 제목", null, null, null);

        // then: contentType 및 memo 유지 검증
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getContentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(updated.getMemo()).isEqualTo("기존 메모");
    }

    @Test
    @DisplayName("delete: Bookmark가 정상 삭제되며 연관된 Tag는 보존된다")
    void deleteBookmarkTest() {
        // given
        Long bookmarkId = 1L;
        Tag tag = new Tag("preserved-tag");
        Bookmark existingBookmark = Bookmark.builder()
                .url("https://example.com")
                .title("삭제 대상 북마크")
                .contentType(ContentType.ARTICLE)
                .tags(Set.of(tag))
                .build();

        given(bookmarkRepository.findById(bookmarkId)).willReturn(Optional.of(existingBookmark));

        // when
        bookmarkService.delete(bookmarkId);

        // then: BookmarkRepository의 delete(bookmark)가 호출되었는지 검증
        verify(bookmarkRepository).delete(existingBookmark);
        // TagRepository의 delete는 호출되지 않음 (Tag 엔티티 자체는 보존)
        verify(tagRepository, never()).delete(any(Tag.class));
    }
}
