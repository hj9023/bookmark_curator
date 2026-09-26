package com.jhj.bookmark_curator.bookmark.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BookmarkNotFoundException 발생 시 404와 BOOKMARK_NOT_FOUND 에러 응답을 반환한다")
    void handleBookmarkNotFoundExceptionTest() {
        BookmarkNotFoundException ex = new BookmarkNotFoundException(1L);

        ResponseEntity<ErrorResponse> response = handler.handleBookmarkNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("BOOKMARK_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("북마크를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("SummaryGenerationException(SUMMARY_RATE_LIMITED) 발생 시 429 에러 응답을 반환한다")
    void handleSummaryRateLimitedExceptionTest() {
        SummaryGenerationException ex = new SummaryGenerationException(ErrorCode.SUMMARY_RATE_LIMITED);

        ResponseEntity<ErrorResponse> response = handler.handleSummaryGenerationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SUMMARY_RATE_LIMITED");
        assertThat(response.getBody().message()).isEqualTo("Groq 요청 한도를 초과했습니다");
    }

    @Test
    @DisplayName("SummaryGenerationException(SUMMARY_TIMEOUT) 발생 시 504 에러 응답을 반환한다")
    void handleSummaryTimeoutExceptionTest() {
        SummaryGenerationException ex = new SummaryGenerationException(ErrorCode.SUMMARY_TIMEOUT);

        ResponseEntity<ErrorResponse> response = handler.handleSummaryGenerationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SUMMARY_TIMEOUT");
        assertThat(response.getBody().message()).isEqualTo("요약 생성 응답이 지연되고 있습니다");
    }

    @Test
    @DisplayName("SummaryGenerationException(SUMMARY_GENERATION_FAILED) 발생 시 502 에러 응답을 반환한다")
    void handleSummaryGenerationFailedExceptionTest() {
        SummaryGenerationException ex = new SummaryGenerationException(ErrorCode.SUMMARY_GENERATION_FAILED);

        ResponseEntity<ErrorResponse> response = handler.handleSummaryGenerationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SUMMARY_GENERATION_FAILED");
        assertThat(response.getBody().message()).isEqualTo("요약 생성에 실패했습니다");
    }

    @Test
    @DisplayName("InvalidUrlException 발생 시 400과 INVALID_URL 에러 응답을 반환한다")
    void handleInvalidUrlExceptionTest() {
        InvalidUrlException ex = new InvalidUrlException();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidUrlException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_URL");
        assertThat(response.getBody().message()).isEqualTo("유효하지 않은 URL입니다");
    }
}

