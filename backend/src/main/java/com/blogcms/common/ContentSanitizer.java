package com.blogcms.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class ContentSanitizer {

    private static final Safelist ARTICLE_SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "alt", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data");

    public String articleHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String safeHtml = Jsoup.clean(value, ARTICLE_SAFELIST);
        return safeHtml.replaceAll("(?i)<a ", "<a rel=\"noopener noreferrer\" ");
    }

    public String plainText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Jsoup.clean(value, Safelist.none()).trim();
    }
}
