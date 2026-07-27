package com.blogcms.comment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByNewsIdAndStatusOrderByCreatedAtDesc(Long newsId, String status);

    List<Comment> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    @Modifying
    void deleteByNewsId(Long newsId);
}
