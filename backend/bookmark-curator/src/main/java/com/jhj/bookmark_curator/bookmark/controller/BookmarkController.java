package com.jhj.bookmark_curator.bookmark.controller;

import com.jhj.bookmark_curator.bookmark.domain.Bookmark;
import com.jhj.bookmark_curator.bookmark.domain.ContentType;
import com.jhj.bookmark_curator.bookmark.dto.BookmarkRequest;
import com.jhj.bookmark_curator.bookmark.dto.BookmarkResponse;
import com.jhj.bookmark_curator.bookmark.service.BookmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody BookmarkRequest request) {
        Bookmark created = bookmarkService.create(request.url(), request.title(), request.memo());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookmarkResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> findAll(
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword
    ) {
        List<BookmarkResponse> responses = bookmarkService.findAll(contentType, tag, keyword).stream()
                .map(BookmarkResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookmarkResponse> update(
            @PathVariable Long id,
            @RequestBody BookmarkRequest request
    ) {
        Bookmark updated = bookmarkService.update(
                id,
                request.title(),
                request.memo(),
                request.contentType(),
                request.tags()
        );
        return ResponseEntity.ok(BookmarkResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookmarkService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
