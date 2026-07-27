package com.blogcms.reaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByNewsIdAndIpHash(Long newsId, String ipHash);

    @Modifying
    void deleteByNewsId(Long newsId);

    long countByNewsIdAndReactionType(Long newsId, String reactionType);

    @Query("""
            SELECT r.newsId, r.reactionType, COUNT(r)
            FROM Reaction r
            WHERE r.newsId IN :newsIds
            GROUP BY r.newsId, r.reactionType
            """)
    List<Object[]> countByNewsIdInGrouped(@Param("newsIds") List<Long> newsIds);
}
