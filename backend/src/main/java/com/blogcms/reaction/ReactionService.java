package com.blogcms.reaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import com.blogcms.news.News;
import com.blogcms.news.NewsRepository;
import com.blogcms.news.NewsStatus;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReactionService {

    public static final String LIKE = "like";
    public static final String DISLIKE = "dislike";

    private final ReactionRepository reactionRepository;
    private final NewsRepository newsRepository;

    public ReactionService(ReactionRepository reactionRepository, NewsRepository newsRepository) {
        this.reactionRepository = reactionRepository;
        this.newsRepository = newsRepository;
    }

    @Transactional
    public ReactionResponseDto react(Long newsId, ReactionRequestDto request, HttpServletRequest servletRequest) {
        String reactionType = normalizeReactionType(request == null ? null : request.getReactionType());
        News news = newsRepository.findById(newsId)
                .filter(this::isPubliclyVisible)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "published news not found"));
        String ipHash = hashClientIp(clientIp(servletRequest));

        Reaction reaction = reactionRepository.findByNewsIdAndIpHash(news.getId(), ipHash)
                .orElseGet(() -> {
                    Reaction created = new Reaction();
                    created.setNews(news);
                    created.setIpHash(ipHash);
                    return created;
                });

        // The unique (news_id, ip_hash) key prevents duplicate reactions; updates let the same IP change choice.
        reaction.setReactionType(reactionType);
        reactionRepository.save(reaction);

        return counts(news.getId(), reactionType);
    }

    public ReactionResponseDto counts(Long newsId, String reactionType) {
        return new ReactionResponseDto(
                newsId,
                reactionRepository.countByNewsIdAndReactionType(newsId, LIKE),
                reactionRepository.countByNewsIdAndReactionType(newsId, DISLIKE),
                reactionType);
    }

    private String normalizeReactionType(String reactionType) {
        if (reactionType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reactionType must be like or dislike");
        }

        String normalized = reactionType.trim().toLowerCase();
        if (!LIKE.equals(normalized) && !DISLIKE.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reactionType must be like or dislike");
        }

        return normalized;
    }

    private boolean isPubliclyVisible(News news) {
        if (!NewsStatus.PUBLISHED.equals(news.getStatus())) {
            return false;
        }

        LocalDateTime publishDate = news.getPublishDate() != null
                ? news.getPublishDate()
                : news.getScheduledAt() != null ? news.getScheduledAt() : news.getCreatedAt();
        return publishDate == null || !publishDate.isAfter(LocalDateTime.now());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String hashClientIp(String clientIp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(clientIp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
