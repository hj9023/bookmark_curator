package com.jhj.bookmark_curator.bookmark.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다"),
    INVALID_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 URL입니다"),
    SUMMARY_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Groq 요청 한도를 초과했습니다"),
    SUMMARY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "요약 생성 응답이 지연되고 있습니다"),
    SUMMARY_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "요약 생성에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}

