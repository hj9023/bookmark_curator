package com.jhj.bookmark_curator.bookmark.exception;

import lombok.Getter;

@Getter
public class SummaryGenerationException extends RuntimeException {

    private final ErrorCode errorCode;

    public SummaryGenerationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SummaryGenerationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public SummaryGenerationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

