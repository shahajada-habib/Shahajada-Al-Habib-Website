package com.blogcms.seo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blogcms.news.News;
import com.blogcms.news.NewsRepository;
import com.blogcms.news.NewsStatus;

@RestController
public class RssFeedController {
    private static final DateTimeFormatter RFC_822 = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);

    private final NewsRepository newsRepository;
    private final String siteUrl;

    public RssFeedController(
            NewsRepository newsRepository,
            @Value("${SITE_URL:https://your-domain.example.com}") String siteUrl) {
        this.newsRepository = newsRepository;
        this.siteUrl = normalizeSiteUrl(siteUrl);
    }

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml; charset=UTF-8")
    @Transactional(readOnly = true)
    public String rss() {
        List<News> articles = newsRepository.findVisiblePublished(
                NewsStatus.PUBLISHED,
                LocalDateTime.now(),
                PageRequest.of(0, 20)).getContent();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\">\n");
        xml.append("  <channel>\n");
        appendElement(xml, "title", "শাহজাদা আল হাবীব", 4);
        appendElement(xml, "link", siteUrl, 4);
        appendElement(xml, "description", "শাহজাদা আল হাবীবের কবিতা, গল্প ও জার্নাল।", 4);
        appendElement(xml, "language", "bn", 4);

        for (News article : articles) {
            xml.append("    <item>\n");
            appendElement(xml, "title", article.getTitle(), 6);
            appendElement(xml, "link", siteUrl + "/article/" + article.getSlug(), 6);
            appendElement(xml, "description", description(article), 6);
            appendElement(xml, "pubDate", pubDate(article), 6);
            appendElement(xml, "guid", siteUrl + "/article/" + article.getSlug(), 6);
            if (article.getCategory() != null) {
                appendElement(xml, "category", article.getCategory().getName(), 6);
            }
            appendElement(xml, "author", authorName(article), 6);
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    private String description(News article) {
        String text = firstNonBlank(article.getSubtitle(), article.getSeoDescription(), article.getContent(), article.getTitle());
        String plainText = text.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        return plainText.length() > 200 ? plainText.substring(0, 200) : plainText;
    }

    private String pubDate(News article) {
        LocalDateTime date = firstNonNull(article.getPublishDate(), article.getScheduledAt(), article.getCreatedAt());
        if (date == null) {
            date = LocalDateTime.now();
        }
        return date.atZone(ZoneId.systemDefault()).format(RFC_822);
    }

    private String authorName(News article) {
        if (article.getAuthor() != null) {
            return firstNonBlank(article.getAuthor().getDisplayName(), article.getAuthor().getUsername());
        }
        return firstNonBlank(article.getReporterName(), article.getSource(), "শাহজাদা আল হাবীব");
    }

    private void appendElement(StringBuilder xml, String name, String value, int indent) {
        xml.append(" ".repeat(indent))
                .append("<").append(name).append(">")
                .append(escape(value))
                .append("</").append(name).append(">\n");
    }

    private String normalizeSiteUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://your-domain.example.com" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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
