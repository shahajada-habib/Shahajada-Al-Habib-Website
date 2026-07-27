package com.blogcms.news;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByStatusOrderByIdDesc(String status);

    Page<News> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("""
            SELECT COUNT(n)
            FROM News n
            WHERE n.status = :status
              AND n.publishDate >= :start
              AND n.publishDate < :end
            """)
    long countPublishedBetween(
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT n.id, n.title, n.slug, n.publishDate, n.viewCount,
                   COALESCE(SUM(CASE WHEN r.reactionType = 'like' THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.reactionType = 'dislike' THEN 1 ELSE 0 END), 0)
            FROM News n
            LEFT JOIN Reaction r ON r.news = n
            WHERE n.status = :status
            GROUP BY n.id, n.title, n.slug, n.publishDate, n.viewCount
            ORDER BY n.publishDate DESC, n.id DESC
            """)
    List<Object[]> findRecentlyPublishedDashboardItems(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT n.id, n.title, n.slug, n.publishDate, n.viewCount,
                   COALESCE(SUM(CASE WHEN r.reactionType = 'like' THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.reactionType = 'dislike' THEN 1 ELSE 0 END), 0)
            FROM News n
            LEFT JOIN Reaction r ON r.news = n
            WHERE n.status = :status
            GROUP BY n.id, n.title, n.slug, n.publishDate, n.viewCount
            ORDER BY n.viewCount DESC, n.id DESC
            """)
    List<Object[]> findTopViewedDashboardItems(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT n.id, n.title, n.slug, n.publishDate, n.viewCount,
                   COALESCE(SUM(CASE WHEN r.reactionType = 'like' THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.reactionType = 'dislike' THEN 1 ELSE 0 END), 0)
            FROM News n
            JOIN Reaction r ON r.news = n
            WHERE n.status = :status
            GROUP BY n.id, n.title, n.slug, n.publishDate, n.viewCount
            ORDER BY COUNT(r) DESC, n.id DESC
            """)
    List<Object[]> findTopReactedDashboardItems(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT COALESCE(c.name, c.slug), COUNT(n.id)
            FROM News n
            JOIN n.category c
            WHERE n.status = :status
            GROUP BY c.id, c.name, c.slug
            HAVING COUNT(n.id) > 0
            ORDER BY COUNT(n.id) DESC
            """)
    List<Object[]> findPublishedCategoryBreakdown(@Param("status") String status);

    // Keep scheduled publishing in the database query so paginated public reads never slice in memory.
    @Query("""
            SELECT n FROM News n
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    Page<News> findVisiblePublished(@Param("status") String status, @Param("now") LocalDateTime now, Pageable pageable);

    // Same visibility rule for legacy callers that still request the non-paginated public list.
    @Query("""
            SELECT n FROM News n
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    List<News> findVisiblePublished(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("""
            SELECT n FROM News n
            LEFT JOIN n.category c
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
              AND (
                    LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(n.subtitle, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(n.content, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(c.slug, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    Page<News> searchVisiblePublished(
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
            SELECT n FROM News n
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
              AND (
                    n.author.id = :authorId
                 OR LOWER(COALESCE(n.reporterName, '')) = LOWER(:username)
              )
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    Page<News> findVisiblePublishedByJournalist(
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("authorId") Long authorId,
            @Param("username") String username,
            Pageable pageable);

    @Query("""
            SELECT n FROM News n
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
              AND n.category.slug = :categorySlug
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    Page<News> findVisiblePublishedByCategory(
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("categorySlug") String categorySlug,
            Pageable pageable);

    @Query("""
            SELECT n FROM News n
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= :now
              AND n.category.id = :categoryId
              AND n.id <> :excludeId
            ORDER BY COALESCE(n.publishDate, n.scheduledAt, n.createdAt) DESC, n.id DESC
            """)
    List<News> findRelated(
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("categoryId") Long categoryId,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    Optional<News> findBySlug(String slug);

    Optional<News> findBySlugAndStatus(String slug, String status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE News n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
