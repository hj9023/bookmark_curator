package com.jhj.bookmark_curator.bookmark.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "memo")
    private String memo;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @ManyToMany
    @JoinTable(
        name = "bookmark_tag",
        joinColumns = @JoinColumn(name = "bookmark_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Bookmark(String url, String title, String memo, String summary, ContentType contentType, Set<Tag> tags) {
        this.url = url;
        this.title = title;
        this.memo = memo;
        this.summary = summary;
        this.contentType = contentType;
        if (tags != null) {
            this.tags = new HashSet<>(tags);
        }
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addTag(Tag tag) {
        if (tag != null) {
            this.tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        if (tag != null) {
            this.tags.remove(tag);
        }
    }

    public void update(String title, String memo, ContentType contentType, Set<Tag> tags) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (memo != null) {
            this.memo = memo;
        }
        if (contentType != null) {
            this.contentType = contentType;
        }
        if (tags != null) {
            this.tags.clear();
            this.tags.addAll(tags);
        }
    }
}

