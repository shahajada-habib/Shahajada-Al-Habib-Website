package com.blogcms.press;

/** What the admin panel gets back when it asks the server to read a URL's Open Graph tags. */
public record LinkPreview(String title, String publication, String summary, String imageUrl, String publishedOn) {
}
