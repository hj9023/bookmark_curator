package com.jhj.bookmark_curator.bookmark.exception;

import lombok.Getter;

@Getter
public class InvalidUrlException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.INVALID_URL;

    public InvalidUrlException() {
        super(ErrorCode.INVALID_URL.getMessage());
    }

    public InvalidUrlException(String message) {
        super(message);
    }

    public InvalidUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}

