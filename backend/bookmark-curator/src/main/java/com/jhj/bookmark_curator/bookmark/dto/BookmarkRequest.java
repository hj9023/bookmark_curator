package com.jhj.bookmark_curator.bookmark.dto;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;

import java.util.List;

public record BookmarkRequest(
    String url,
    String title,
    String memo,
    ContentType contentType,
    List<String> tags
) {}

