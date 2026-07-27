package com.blogcms.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.blogcms.comment.CommentRepository;
import com.blogcms.news.NewsRepository;
import com.blogcms.news.NewsStatus;
import com.blogcms.user.UserRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public DashboardController(
            NewsRepository newsRepository,
            UserRepository userRepository,
            CommentRepository commentRepository) {
        this.newsRepository = newsRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    @GetMapping("/stats")
    public DashboardStatsDto getStats() {
        LocalDate today = LocalDate.now();
        return new DashboardStatsDto(
                newsRepository.count(),
                newsRepository.countByStatus(NewsStatus.PUBLISHED),
                newsRepository.countByStatus(NewsStatus.DRAFT),
                newsRepository.countByStatus(NewsStatus.REVIEW),
                userRepository.count(),
                commentRepository.countByStatus("pending"),
                newsRepository.countPublishedBetween(NewsStatus.PUBLISHED, today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                dashboardArticles(newsRepository.findRecentlyPublishedDashboardItems(NewsStatus.PUBLISHED, PageRequest.of(0, 5))),
                dashboardArticles(newsRepository.findTopViewedDashboardItems(NewsStatus.PUBLISHED, PageRequest.of(0, 5))),
                dashboardArticles(newsRepository.findTopReactedDashboardItems(NewsStatus.PUBLISHED, PageRequest.of(0, 5))),
                categoryBreakdown(newsRepository.findPublishedCategoryBreakdown(NewsStatus.PUBLISHED)));
    }

    private List<DashboardStatsDto.DashboardArticleDto> dashboardArticles(List<Object[]> rows) {
        return rows.stream()
                .map((row) -> new DashboardStatsDto.DashboardArticleDto(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        row[3] == null ? "" : ((LocalDateTime) row[3]).toString(),
                        ((Number) row[4]).longValue(),
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).longValue()))
                .toList();
    }

    private List<DashboardStatsDto.CategoryBreakdownDto> categoryBreakdown(List<Object[]> rows) {
        return rows.stream()
                .map((row) -> new DashboardStatsDto.CategoryBreakdownDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
