package com.blogcms.tag;

import java.util.List;

import com.blogcms.security.CurrentUserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TagController {

    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;

    public TagController(TagRepository tagRepository, CurrentUserService currentUserService) {
        this.tagRepository = tagRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/public/tags")
    public List<String> publicTags() {
        return tagRepository.findPublicPublishedTagNames(com.blogcms.news.NewsStatus.PUBLISHED);
    }

    @GetMapping("/api/admin/tags")
    public List<TagSummaryDto> adminTags() {
        currentUserService.requireEditorOrAdmin("view tags");
        return tagRepository.findAllWithArticleCount().stream()
                .map((row) -> new TagSummaryDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
