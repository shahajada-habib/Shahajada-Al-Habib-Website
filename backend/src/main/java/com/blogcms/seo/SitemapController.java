package com.blogcms.seo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blogcms.category.Category;
import com.blogcms.category.CategoryRepository;
import com.blogcms.news.News;
import com.blogcms.news.NewsRepository;
import com.blogcms.news.NewsStatus;
import com.blogcms.user.User;
import com.blogcms.user.UserRepository;

@RestController
public class SitemapController {
    private static final DateTimeFormatter LASTMOD_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final CategoryRepository categoryRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final String siteUrl;

    public SitemapController(
            CategoryRepository categoryRepository,
            NewsRepository newsRepository,
            UserRepository userRepository,
            @Value("${SITE_URL:https://your-domain.example.com}") String siteUrl) {
        this.categoryRepository = categoryRepository;
        this.newsRepository = newsRepository;
        this.userRepository = userRepository;
        this.siteUrl = normalizeSiteUrl(siteUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, "/", "hourly", "1.0", null);
        appendUrl(xml, "/journalists", "monthly", "0.6", null);

        for (Category category : categoryRepository.findAll()) {
            if (category.getSlug() != null && !category.getSlug().isBlank()) {
                appendUrl(xml, "/category/" + category.getSlug(), "daily", "0.7", null);
            }
        }

        List<News> articles = newsRepository.findVisiblePublished(
                NewsStatus.PUBLISHED,
                LocalDateTime.now(),
                PageRequest.of(0, 1000)).getContent();
        for (News article : articles) {
            appendUrl(xml, "/article/" + article.getSlug(), "weekly", "0.9", formatLastModified(article.getUpdatedAt()));
        }

        for (User journalist : userRepository.findPublicJournalists()) {
            appendUrl(xml, "/author/" + journalist.getUsername(), "monthly", "0.6", null);
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String path, String changefreq, String priority, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escape(siteUrl + path)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(escape(lastmod)).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String formatLastModified(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return null;
        }
        return updatedAt.atZone(ZoneId.systemDefault()).format(LASTMOD_FORMATTER);
    }

    private String normalizeSiteUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://your-domain.example.com" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
