package com.jhj.bookmark_curator.bookmark.repository;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.domain.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Bookmark와 Tag를 저장하고 정상적으로 조회할 수 있다")
    void saveAndFindBookmarkWithTag() {
        // given
        Tag tag1 = tagRepository.save(new Tag("  AI  "));
        Tag tag2 = tagRepository.save(new Tag("SPRING"));

        Bookmark bookmark = Bookmark.builder()
                .url("https://example.com/article")
                .title("테스트 아티클 제목")
                .memo("나중에 읽을 메모")
                .summary("1줄 요약\n2줄 요약\n3줄 요약")
                .contentType(ContentType.ARTICLE)
                .tags(Set.of(tag1, tag2))
                .build();

        // when
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<Bookmark> foundBookmarkOptional = bookmarkRepository.findById(savedBookmark.getId());
        assertThat(foundBookmarkOptional).isPresent();

        Bookmark foundBookmark = foundBookmarkOptional.get();
        assertThat(foundBookmark.getUrl()).isEqualTo("https://example.com/article");
        assertThat(foundBookmark.getTitle()).isEqualTo("테스트 아티클 제목");
        assertThat(foundBookmark.getMemo()).isEqualTo("나중에 읽을 메모");
        assertThat(foundBookmark.getSummary()).isEqualTo("1줄 요약\n2줄 요약\n3줄 요약");
        assertThat(foundBookmark.getContentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(foundBookmark.getCreatedAt()).isNotNull();
        assertThat(foundBookmark.getUpdatedAt()).isNotNull();

        // 태그 검증 (정규화 및 다대다 매핑)
        assertThat(foundBookmark.getTags()).hasSize(2);
        assertThat(foundBookmark.getTags())
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("ai", "spring");
    }

    @Test
    @DisplayName("Bookmark 삭제 시 조인 테이블 레코드는 삭제되지만 Tag는 유지된다")
    void deleteBookmarkKeepsTag() {
        // given
        Tag tag = tagRepository.save(new Tag("ai"));
        Bookmark bookmark = Bookmark.builder()
                .url("https://example.com")
                .title("제목")
                .contentType(ContentType.ARTICLE)
                .tags(Set.of(tag))
                .build();

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        entityManager.flush();
        entityManager.clear();

        Long bookmarkId = savedBookmark.getId();
        Long tagId = tag.getId();

        // when
        bookmarkRepository.deleteById(bookmarkId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(bookmarkRepository.findById(bookmarkId)).isEmpty();
        assertThat(tagRepository.findById(tagId)).isPresent();
    }
}
