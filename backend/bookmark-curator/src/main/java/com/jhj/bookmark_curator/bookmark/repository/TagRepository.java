package com.jhj.bookmark_curator.bookmark.repository;

import com.jhj.bookmark_curator.bookmark.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
