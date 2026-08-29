package com.blogcms.dashboard;

import java.util.List;

public record DashboardStatsDto(
        long totalNews,
        long published,
        long draft,
        long review,
        long totalUsers,
        long pendingComments,
        long pendingCvRequests,
        long todayPublished,
        List<DashboardArticleDto> recentlyPublished,
        List<DashboardArticleDto> topViewed,
        List<DashboardArticleDto> topReacted,
        List<CategoryBreakdownDto> categoryBreakdown) {

    public record DashboardArticleDto(
            Long id,
            String title,
            String slug,
            String publishDate,
            long viewCount,
            long likeCount,
            long dislikeCount) {
    }

    public record CategoryBreakdownDto(String categoryName, long publishedCount) {
    }
}
