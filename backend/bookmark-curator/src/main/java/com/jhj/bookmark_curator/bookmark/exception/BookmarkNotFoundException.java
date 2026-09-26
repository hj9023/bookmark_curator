package com.jhj.bookmark_curator.bookmark.exception;

import lombok.Getter;

@Getter
public class BookmarkNotFoundException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.BOOKMARK_NOT_FOUND;

    public BookmarkNotFoundException() {
        super(ErrorCode.BOOKMARK_NOT_FOUND.getMessage());
    }

    public BookmarkNotFoundException(Long id) {
        super(ErrorCode.BOOKMARK_NOT_FOUND.getMessage() + " (id=" + id + ")");
    }

    public BookmarkNotFoundException(String message) {
        super(message);
    }
}

