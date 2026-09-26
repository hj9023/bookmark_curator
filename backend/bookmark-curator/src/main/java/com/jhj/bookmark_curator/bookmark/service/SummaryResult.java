package com.jhj.bookmark_curator.bookmark.service;

import java.util.List;

public record SummaryResult(
    String summary,
    List<String> tags
) {}
