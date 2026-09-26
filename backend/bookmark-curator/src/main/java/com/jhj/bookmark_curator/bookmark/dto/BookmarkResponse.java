package com.jhj.bookmark_curator.bookmark.dto;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.domain.Tag;

import java.time.LocalDateTime;
import java.util.List;

public record BookmarkResponse(
    Long id,
    String url,
    String title,
    String memo,
    String summary,
    ContentType contentType,
    List<String> tags,
    LocalDateTime createdAt
) {
    public static BookmarkResponse from(Bookmark bookmark) {
        List<String> tagNames = bookmark.getTags() != null
                ? bookmark.getTags().stream().map(Tag::getName).sorted().toList()
                : List.of();

        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getUrl(),
                bookmark.getTitle(),
                bookmark.getMemo(),
                bookmark.getSummary(),
                bookmark.getContentType(),
                tagNames,
                bookmark.getCreatedAt()
        );
    }
}
