package com.blogcms.user;

import java.util.List;

import com.blogcms.common.PageResponse;
import com.blogcms.news.NewsResponseDto;
import com.blogcms.news.NewsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/journalists")
public class PublicJournalistController {

    private final UserService userService;
    private final NewsService newsService;

    public PublicJournalistController(UserService userService, NewsService newsService) {
        this.userService = userService;
        this.newsService = newsService;
    }

    @GetMapping
    public List<UserResponseDto> getJournalists() {
        return userService.getAllPublicJournalists();
    }

    @GetMapping("/{username}")
    public UserResponseDto getJournalist(@PathVariable String username) {
        return userService.getUserProfile(username);
    }

    @GetMapping("/{username}/articles")
    public PageResponse<NewsResponseDto> getJournalistArticles(
            @PathVariable String username,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return newsService.getPublishedNewsByJournalist(username, page, size);
    }
}
