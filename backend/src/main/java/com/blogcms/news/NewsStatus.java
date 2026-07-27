package com.blogcms.news;

import java.util.Set;

public final class NewsStatus {
    public static final String DRAFT = "draft";
    public static final String REVIEW = "review";
    public static final String PUBLISHED = "published";
    public static final String ARCHIVED = "archived";

    public static final Set<String> ALL = Set.of(DRAFT, REVIEW, PUBLISHED, ARCHIVED);

    private NewsStatus() {
    }
}
