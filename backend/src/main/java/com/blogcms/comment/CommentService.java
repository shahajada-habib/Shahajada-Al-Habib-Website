package com.blogcms.comment;

import java.util.List;
import java.util.Set;

import com.blogcms.common.ContentSanitizer;
import com.blogcms.common.InputValidator;
import com.blogcms.news.News;
import com.blogcms.news.NewsRepository;
import com.blogcms.security.CurrentUserService;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "approved", "rejected");

    private final CommentRepository commentRepository;
    private final NewsRepository newsRepository;
    private final CurrentUserService currentUserService;
    private final ContentSanitizer sanitizer;
    private final InputValidator inputValidator;

    public CommentService(
            CommentRepository commentRepository,
            NewsRepository newsRepository,
            CurrentUserService currentUserService,
            ContentSanitizer sanitizer,
            InputValidator inputValidator) {
        this.commentRepository = commentRepository;
        this.newsRepository = newsRepository;
        this.currentUserService = currentUserService;
        this.sanitizer = sanitizer;
        this.inputValidator = inputValidator;
    }

    public CommentResponseDto createPublicComment(Long newsId, CommentRequestDto request) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "news not found"));

        Comment comment = new Comment();
        comment.setNews(news);
        comment.setAuthor(inputValidator.required(sanitizer.plainText(request.getAuthor()), "author", 80));
        comment.setContent(inputValidator.required(sanitizer.plainText(request.getContent()), "comment", 2000));
        comment.setStatus("pending");
        Comment savedComment = commentRepository.save(comment);
        log.info("NEW_COMMENT_PENDING newsId={} author={} commentId={}", newsId, savedComment.getAuthor(), savedComment.getId());
        return toResponse(savedComment);
    }

    public List<CommentResponseDto> getApprovedForNews(Long newsId) {
        return commentRepository.findByNewsIdAndStatusOrderByCreatedAtDesc(newsId, "approved").stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CommentResponseDto> getAdminComments() {
        currentUserService.requireEditorOrAdmin("list comments");
        return commentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public CommentResponseDto approveComment(Long id) {
        currentUserService.requireEditorOrAdmin("approve comments");
        Comment comment = getComment(id);
        comment.setStatus("approved");
        return toResponse(commentRepository.save(comment));
    }

    public void deleteComment(Long id) {
        currentUserService.requireEditorOrAdmin("delete comments");
        if (!commentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found");
        }
        commentRepository.deleteById(id);
    }

    private Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
    }

    private CommentResponseDto toResponse(Comment comment) {
        CommentResponseDto response = new CommentResponseDto();
        response.setId(comment.getId());
        response.setNewsId(comment.getNews().getId());
        response.setArticleTitle(comment.getNews().getTitle());
        response.setAuthor(comment.getAuthor());
        response.setContent(comment.getContent());
        response.setStatus(normalizeStatus(comment.getStatus()));
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }

    private String normalizeStatus(String value) {
        String status = value == null ? "pending" : value.trim().toLowerCase();
        return ALLOWED_STATUSES.contains(status) ? status : "pending";
    }

}
