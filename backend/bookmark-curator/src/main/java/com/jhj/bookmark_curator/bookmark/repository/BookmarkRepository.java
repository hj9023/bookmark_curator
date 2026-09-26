package com.jhj.bookmark_curator.bookmark.repository;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findAllByContentType(ContentType contentType);

    @Query("SELECT DISTINCT b FROM Bookmark b " +
           "LEFT JOIN b.tags t " +
           "WHERE (:contentType IS NULL OR b.contentType = :contentType) " +
           "AND (:tag IS NULL OR LOWER(t.name) = LOWER(:tag)) " +
           "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.memo) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY b.createdAt DESC")
    List<Bookmark> search(@Param("contentType") ContentType contentType,
                         @Param("tag") String tag,
                         @Param("keyword") String keyword);
}
