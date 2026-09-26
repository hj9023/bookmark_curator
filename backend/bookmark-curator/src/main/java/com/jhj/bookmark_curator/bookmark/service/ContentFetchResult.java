package com.jhj.bookmark_curator.bookmark.service;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;

public record ContentFetchResult(
    ContentType contentType,
    String title,
    String content
) {
    public static ContentFetchResult emptyOther() {
        return new ContentFetchResult(ContentType.OTHER, "", "");
    }
}
