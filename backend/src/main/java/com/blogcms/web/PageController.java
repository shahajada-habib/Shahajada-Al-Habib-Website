package com.blogcms.web;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.blogcms.category.Category;
import com.blogcms.category.CategoryRepository;
import com.blogcms.comment.CommentRequestDto;
import com.blogcms.comment.CommentResponseDto;
import com.blogcms.comment.CommentService;
import com.blogcms.common.PageResponse;
import com.blogcms.cvrequest.CvRequestService;
import com.blogcms.media.MediaAssetService;
import com.blogcms.news.NewsResponseDto;
import com.blogcms.news.NewsService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
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
    private final CvRequestService cvRequestService;
    private final MediaAssetService mediaAssetService;
    private final MessageSource messageSource;
    private final String siteUrl;

    public PageController(
            NewsService newsService,
            CategoryRepository categoryRepository,
            CommentService commentService,
            CvRequestService cvRequestService,
            MediaAssetService mediaAssetService,
            MessageSource messageSource,
            @Value("${app.site-url:}") String siteUrl) {
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
        this.newsService = newsService;
        this.categoryRepository = categoryRepository;
        this.commentService = commentService;
        this.cvRequestService = cvRequestService;
        this.mediaAssetService = mediaAssetService;
        this.messageSource = messageSource;
    }

    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    @ModelAttribute
    public void addSharedAttributes(Model model) {
        model.addAttribute("navCategories", categoryRepository.findByStatusOrderByIdAsc("active"));
        model.addAttribute("pageImage", (Object) null);
        // Share bots need absolute canonical/og URLs; templates prepend this to the page path.
        model.addAttribute("siteUrl", siteUrl);
        // Only the article page is a real og:type=article; everything else is a site page.
        model.addAttribute("pageType", "website");
    }

    @GetMapping("/")
    public String home(Model model, Locale locale) {
        PageResponse<NewsResponseDto> latest = pageResponse(newsService.getPublishedNews(0, PAGE_SIZE));

        List<NewsResponseDto> featured = latest.content().stream().filter(NewsResponseDto::isFeatured).limit(4).toList();
        // Hide the Featured section when it would just duplicate the Latest section below it
        // (e.g. with only one published article total).
        model.addAttribute("featured", featured.size() < latest.content().size() ? featured : List.of());
        model.addAttribute("latest", latest.content());
        model.addAttribute("pageTitle", msg("nav.home", locale));
        model.addAttribute("pageDescription", msg("page.home.description", locale));
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
        model.addAttribute("pageType", "article");
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
    public String search(@RequestParam(required = false, defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model, Locale locale) {
        PageResponse<NewsResponseDto> result = newsService.searchPublishedNews(q, page, PAGE_SIZE);

        model.addAttribute("query", q);
        model.addAttribute("articles", result.content());
        model.addAttribute("currentPage", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("pageTitle", q.isBlank() ? msg("search.allArticles", locale) : msg("search.resultsFor", locale, q));
        model.addAttribute("pageDescription", q.isBlank() ? msg("page.search.description", locale) : msg("page.search.descriptionQuery", locale, q));
        model.addAttribute("pageUrl", "/search");
        return "search";
    }

    @GetMapping("/about")
    public String about(Model model, Locale locale) {
        model.addAttribute("pageTitle", msg("nav.about", locale));
        model.addAttribute("pageDescription", msg("page.about.description", locale));
        model.addAttribute("pageUrl", "/about");
        return "about";
    }

    @PostMapping("/about/cv-request")
    public String submitCvRequest(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String purpose,
            @RequestParam(name = "website", required = false, defaultValue = "") String honeypot,
            RedirectAttributes redirectAttributes) {
        cvRequestService.create(name, email, purpose, honeypot);
        redirectAttributes.addFlashAttribute("cvRequestSubmitted", true);
        return "redirect:/about#cv";
    }

    @GetMapping("/gallery")
    public String gallery(Model model, Locale locale) {
        model.addAttribute("photos", mediaAssetService.getGallery());
        model.addAttribute("pageTitle", msg("gallery.title", locale));
        model.addAttribute("pageDescription", msg("gallery.subtitle", locale));
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
