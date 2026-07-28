package com.blogcms.web;

import java.util.List;
import java.util.Optional;

import com.blogcms.category.Category;
import com.blogcms.category.CategoryRepository;
import com.blogcms.comment.CommentRequestDto;
import com.blogcms.comment.CommentResponseDto;
import com.blogcms.comment.CommentService;
import com.blogcms.common.PageResponse;
import com.blogcms.media.MediaAssetService;
import com.blogcms.news.NewsResponseDto;
import com.blogcms.news.NewsService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered public site. Kept deliberately separate from the REST API
 * controllers (/api/**) — this is what search engines and social-share bots
 * see, so every page needs real HTML with correct meta tags on first response.
 */
@Controller
public class PageController {

    private static final int PAGE_SIZE = 10;
    private static final int RELATED_LIMIT = 4;

    private final NewsService newsService;
    private final CategoryRepository categoryRepository;
    private final CommentService commentService;
    private final MediaAssetService mediaAssetService;

    public PageController(
            NewsService newsService,
            CategoryRepository categoryRepository,
            CommentService commentService,
            MediaAssetService mediaAssetService) {
        this.newsService = newsService;
        this.categoryRepository = categoryRepository;
        this.commentService = commentService;
        this.mediaAssetService = mediaAssetService;
    }

    @ModelAttribute
    public void addSharedAttributes(Model model) {
        model.addAttribute("navCategories", categoryRepository.findByStatusOrderByIdAsc("active"));
        model.addAttribute("pageImage", (Object) null);
    }

    @GetMapping("/")
    public String home(Model model) {
        PageResponse<NewsResponseDto> latest = pageResponse(newsService.getPublishedNews(0, PAGE_SIZE));

        List<NewsResponseDto> featured = latest.content().stream().filter(NewsResponseDto::isFeatured).limit(4).toList();
        // Hide the Featured section when it would just duplicate the Latest section below it
        // (e.g. with only one published article total).
        model.addAttribute("featured", featured.size() < latest.content().size() ? featured : List.of());
        model.addAttribute("latest", latest.content());
        model.addAttribute("pageTitle", "হোম");
        model.addAttribute("pageDescription", "শাহজাদা আল হাবীবের কবিতা, গল্প ও জার্নাল।");
        model.addAttribute("pageUrl", "/");
        return "index";
    }

    @GetMapping("/category/{slug}")
    public String category(@PathVariable String slug, @RequestParam(defaultValue = "0") int page, Model model) {
        Category category = categoryRepository.findBySlug(slug).orElse(null);
        if (category == null) {
            return "redirect:/";
        }

        PageResponse<NewsResponseDto> result = newsService.getPublishedNewsByCategory(slug, page, PAGE_SIZE);

        model.addAttribute("category", category);
        model.addAttribute("articles", result.content());
        model.addAttribute("currentPage", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("pageTitle", category.getName());
        model.addAttribute("pageDescription", category.getName() + " — সব লেখা");
        model.addAttribute("pageUrl", "/category/" + slug);
        return "category";
    }

    @GetMapping("/article/{slug}")
    public String article(@PathVariable String slug, Model model) {
        Optional<NewsResponseDto> articleOpt = newsService.getPublishedNewsBySlug(slug);
        if (articleOpt.isEmpty()) {
            return "redirect:/";
        }
        NewsResponseDto article = newsService.incrementViewCount(articleOpt.get().getId()).orElse(articleOpt.get());

        List<CommentResponseDto> comments = commentService.getApprovedForNews(article.getId());
        List<NewsResponseDto> related = newsService.getRelatedPublishedNews(slug, RELATED_LIMIT);

        model.addAttribute("article", article);
        model.addAttribute("comments", comments);
        model.addAttribute("related", related);
        model.addAttribute("pageTitle", nonBlank(article.getSeoTitle(), article.getTitle()));
        model.addAttribute("pageDescription", nonBlank(article.getSeoDescription(), article.getSubtitle()));
        model.addAttribute("pageImage", article.getImageUrl());
        model.addAttribute("pageUrl", "/article/" + slug);
        return "article";
    }

    @PostMapping("/article/{slug}/comments")
    public String submitComment(
            @PathVariable String slug,
            @RequestParam String author,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {
        Optional<NewsResponseDto> articleOpt = newsService.getPublishedNewsBySlug(slug);
        if (articleOpt.isPresent()) {
            CommentRequestDto request = new CommentRequestDto();
            request.setAuthor(author);
            request.setContent(content);
            commentService.createPublicComment(articleOpt.get().getId(), request);
            redirectAttributes.addFlashAttribute("commentSubmitted", true);
        }
        return "redirect:/article/" + slug + "#comments";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        PageResponse<NewsResponseDto> result = newsService.searchPublishedNews(q, page, PAGE_SIZE);

        model.addAttribute("query", q);
        model.addAttribute("articles", result.content());
        model.addAttribute("currentPage", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("pageTitle", q.isBlank() ? "খুঁজুন" : "অনুসন্ধান: " + q);
        model.addAttribute("pageDescription", "লেখা খুঁজুন" + (q.isBlank() ? "" : " \"" + q + "\" এর জন্য"));
        model.addAttribute("pageUrl", "/search");
        return "search";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "আমার সম্পর্কে");
        model.addAttribute("pageDescription", "শাহজাদা আল হাবীব — লেখক, কবি ও ভ্রমণপ্রেমী। প্রথম কাব্যগ্রন্থ: অশ্রুচুক্তি (২০২৩)।");
        model.addAttribute("pageUrl", "/about");
        return "about";
    }

    @GetMapping("/gallery")
    public String gallery(Model model) {
        model.addAttribute("photos", mediaAssetService.getGallery());
        model.addAttribute("pageTitle", "গ্যালারি");
        model.addAttribute("pageDescription", "বইমেলা, ভ্রমণ ও ব্যক্তিগত স্মৃতির কিছু ঝলক।");
        model.addAttribute("pageUrl", "/gallery");
        return "gallery";
    }

    @GetMapping({"/admin", "/admin/"})
    public String admin() {
        return "redirect:/admin/index.html";
    }

    @SuppressWarnings("unchecked")
    private PageResponse<NewsResponseDto> pageResponse(Object result) {
        // getPublishedNews(page, size) always returns a PageResponse when both args are non-null.
        return (PageResponse<NewsResponseDto>) result;
    }

    private String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
