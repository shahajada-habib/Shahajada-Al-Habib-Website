package com.blogcms.news;

import com.blogcms.reaction.ReactionRequestDto;
import com.blogcms.reaction.ReactionResponseDto;
import com.blogcms.reaction.ReactionService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/news")
public class PublicNewsController {

    private final NewsService newsService;
    private final ReactionService reactionService;

    public PublicNewsController(NewsService newsService, ReactionService reactionService) {
        this.newsService = newsService;
        this.reactionService = reactionService;
    }

    @GetMapping
    public Object getPublishedNews(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return newsService.getPublishedNews(page, size);
    }

    @GetMapping("/category/{slug}")
    public Object getPublishedNewsByCategory(
            @PathVariable String slug,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return newsService.getPublishedNewsByCategory(slug, page, size);
    }

    @GetMapping("/search")
    public Object searchPublishedNews(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return newsService.searchPublishedNews(q, page, size);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<NewsResponseDto> getPublishedNewsBySlug(@PathVariable String slug) {
        return newsService.getPublishedNewsBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/view")
    public ResponseEntity<Void> registerView(@PathVariable String slug) {
        return newsService.getPublishedNewsBySlug(slug)
                .map((article) -> {
                    newsService.incrementViewCount(article.getId());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{newsId}/reaction")
    public ReactionResponseDto reactToNews(
            @PathVariable Long newsId,
            @RequestBody ReactionRequestDto request,
            HttpServletRequest servletRequest) {
        return reactionService.react(newsId, request, servletRequest);
    }
}
