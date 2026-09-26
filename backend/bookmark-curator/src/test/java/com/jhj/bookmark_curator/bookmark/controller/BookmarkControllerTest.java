package com.jhj.bookmark_curator.bookmark.controller;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.domain.Tag;
import com.jhj.bookmark_curator.bookmark.dto.BookmarkRequest;
import com.jhj.bookmark_curator.bookmark.exception.BookmarkNotFoundException;
import com.jhj.bookmark_curator.bookmark.exception.GlobalExceptionHandler;
import com.jhj.bookmark_curator.bookmark.service.BookmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookmarkController.class)
@Import(GlobalExceptionHandler.class)
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookmarkService bookmarkService;

    private Bookmark createSampleBookmark(Long id, String url, String title, ContentType contentType) {
        Bookmark bookmark = Bookmark.builder()
                .url(url)
                .title(title)
                .memo("테스트 메모")
                .summary("테스트 요약 내용")
                .contentType(contentType)
                .tags(Set.of(new Tag("spring"), new Tag("ai")))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(bookmark, "id", id);
        return bookmark;
    }

    @Test
    @DisplayName("POST /api/bookmarks: 유효한 요청 시 201 Created 및 BookmarkResponse 반환")
    void createBookmark_success() throws Exception {
        // given
        Bookmark bookmark = createSampleBookmark(1L, "https://example.com/article", "새 아티클", ContentType.ARTICLE);
        given(bookmarkService.create(eq("https://example.com/article"), eq("새 아티클"), eq("테스트 메모")))
                .willReturn(bookmark);

        BookmarkRequest request = new BookmarkRequest("https://example.com/article", "새 아티클", "테스트 메모", null, null);

        // when & then
        mockMvc.perform(post("/api/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.url").value("https://example.com/article"))
                .andExpect(jsonPath("$.title").value("새 아티클"))
                .andExpect(jsonPath("$.summary").value("테스트 요약 내용"))
                .andExpect(jsonPath("$.contentType").value("ARTICLE"))
                .andExpect(jsonPath("$.tags").isArray());
    }

    @Test
    @DisplayName("POST /api/bookmarks: 잘못된 URL 형식일 경우 400 Bad Request 및 INVALID_URL 응답")
    void createBookmark_invalidUrl() throws Exception {
        BookmarkRequest request = new BookmarkRequest("invalid-not-a-url", "제목", "메모", null, null);

        mockMvc.perform(post("/api/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 URL입니다"));
    }

    @Test
    @DisplayName("POST /api/bookmarks: URL이 비어있는 경우 400 Bad Request 및 INVALID_URL 응답")
    void createBookmark_blankUrl() throws Exception {
        BookmarkRequest request = new BookmarkRequest("", "제목", "메모", null, null);

        mockMvc.perform(post("/api/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL"));
    }

    @Test
    @DisplayName("GET /api/bookmarks: 쿼리 파라미터 필터링 조회 시 200 OK 반환")
    void findAllBookmarks_withParams() throws Exception {
        Bookmark bookmark = createSampleBookmark(1L, "https://example.com/article", "아티클", ContentType.ARTICLE);
        given(bookmarkService.findAll(eq(ContentType.ARTICLE), eq("spring"), eq("검색어")))
                .willReturn(List.of(bookmark));

        mockMvc.perform(get("/api/bookmarks")
                        .param("contentType", "ARTICLE")
                        .param("tag", "spring")
                        .param("keyword", "검색어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].contentType").value("ARTICLE"));
    }

    @Test
    @DisplayName("GET /api/bookmarks: 파라미터 없이 전체 조회 시 200 OK 반환")
    void findAllBookmarks_noParams() throws Exception {
        Bookmark bookmark = createSampleBookmark(1L, "https://example.com/article", "아티클", ContentType.ARTICLE);
        given(bookmarkService.findAll(isNull(), isNull(), isNull()))
                .willReturn(List.of(bookmark));

        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/bookmarks/{id}: 정상 수정 시 200 OK 및 수정된 BookmarkResponse 반환")
    void updateBookmark_success() throws Exception {
        Bookmark updated = createSampleBookmark(1L, "https://example.com/article", "수정된 제목", ContentType.VIDEO);
        given(bookmarkService.update(eq(1L), eq("수정된 제목"), eq("수정된 메모"), eq(ContentType.VIDEO), anyList()))
                .willReturn(updated);

        BookmarkRequest request = new BookmarkRequest(
                null,
                "수정된 제목",
                "수정된 메모",
                ContentType.VIDEO,
                List.of("video-tag")
        );

        mockMvc.perform(patch("/api/bookmarks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.contentType").value("VIDEO"));
    }

    @Test
    @DisplayName("PATCH /api/bookmarks/{id}: 존재하지 않는 id일 때 404 Not Found 및 BOOKMARK_NOT_FOUND 응답")
    void updateBookmark_notFound() throws Exception {
        given(bookmarkService.update(eq(999L), any(), any(), any(), any()))
                .willThrow(new BookmarkNotFoundException(999L));

        BookmarkRequest request = new BookmarkRequest(null, "수정 제목", null, null, null);

        mockMvc.perform(patch("/api/bookmarks/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKMARK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("북마크를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("DELETE /api/bookmarks/{id}: 정상 삭제 시 204 No Content 반환")
    void deleteBookmark_success() throws Exception {
        willDoNothing().given(bookmarkService).delete(1L);

        mockMvc.perform(delete("/api/bookmarks/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/bookmarks/{id}: 존재하지 않는 id 삭제 시 404 Not Found 및 BOOKMARK_NOT_FOUND 응답")
    void deleteBookmark_notFound() throws Exception {
        willThrow(new BookmarkNotFoundException(999L)).given(bookmarkService).delete(999L);

        mockMvc.perform(delete("/api/bookmarks/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKMARK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("북마크를 찾을 수 없습니다"));
    }
}
