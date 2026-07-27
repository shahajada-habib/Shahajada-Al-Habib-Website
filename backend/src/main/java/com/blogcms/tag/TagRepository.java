package com.blogcms.tag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT DISTINCT t.name
            FROM News n
            JOIN n.tagEntities t
            WHERE n.status = :status
              AND COALESCE(n.publishDate, n.scheduledAt, n.createdAt) <= CURRENT_TIMESTAMP
            ORDER BY t.name
            """)
    List<String> findPublicPublishedTagNames(@Param("status") String status);

    @Query("""
            SELECT t.name, (
                SELECT COUNT(n.id)
                FROM News n
                JOIN n.tagEntities nt
                WHERE nt = t
            )
            FROM Tag t
            ORDER BY t.name
            """)
    List<Object[]> findAllWithArticleCount();
}
