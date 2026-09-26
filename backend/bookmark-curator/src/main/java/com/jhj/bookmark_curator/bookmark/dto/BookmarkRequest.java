package com.jhj.bookmark_curator.bookmark.dto;

import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record BookmarkRequest(
    @NotBlank(message = "URL은 필수입니다")
    @URL(message = "유효하지 않은 URL입니다")
    String url,
    String title,
    String memo,
    ContentType contentType,
    List<String> tags
) {}

