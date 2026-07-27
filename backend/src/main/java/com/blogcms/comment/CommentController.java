package com.blogcms.comment;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/public/news/{newsId}/comments")
    public CommentResponseDto createPublicComment(
            @PathVariable Long newsId,
            @RequestBody CommentRequestDto request) {
        return commentService.createPublicComment(newsId, request);
    }

    @GetMapping("/api/public/news/{newsId}/comments")
    public List<CommentResponseDto> getApprovedComments(@PathVariable Long newsId) {
        return commentService.getApprovedForNews(newsId);
    }

    @GetMapping("/api/admin/comments")
    public List<CommentResponseDto> getAdminComments() {
        return commentService.getAdminComments();
    }

    @PatchMapping("/api/admin/comments/{id}/approve")
    public CommentResponseDto approveComment(@PathVariable Long id) {
        return commentService.approveComment(id);
    }

    @DeleteMapping("/api/admin/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
