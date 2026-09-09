package com.blogcms.press;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads a page's Open Graph tags so the admin panel can pre-fill a clipping
 * from just a URL.
 *
 * <p>This makes the server fetch a URL a user supplied, so every hop is checked
 * before it is followed: http(s) only, and never an address that resolves onto
 * the machine's own network. That last rule matters most on the GCP VM, where
 * 169.254.169.254 would otherwise hand out instance credentials.
 */
@Component
public class LinkPreviewFetcher {

    private static final Logger log = LoggerFactory.getLogger(LinkPreviewFetcher.class);

    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 4;
    // Some papers serve a stripped page to unknown agents; a normal UA gets the real tags.
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; ShahajadaAlHabibSite/1.0; +https://shahajadaalhabib.com)";

    public LinkPreview fetch(String rawUrl) {
        URI uri = validated(rawUrl);

        Document document = null;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Connection.Response response;
            try {
                response = Jsoup.connect(uri.toString())
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(MAX_BODY_BYTES)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .followRedirects(false)
                        .execute();
            } catch (IOException exception) {
                log.warn("Link preview fetch failed url={} reason={}", uri, exception.toString());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "লিংকটি পড়া গেল না — তথ্যগুলো হাতে লিখে দিন");
            }

            String location = response.header("Location");
            if (response.statusCode() / 100 == 3 && location != null && !location.isBlank()) {
                // Re-validate the redirect target: a public URL can still bounce inward.
                uri = validated(uri.resolve(location).toString());
                continue;
            }

            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "লিংকটি খোলা গেল না (HTTP " + response.statusCode() + ") — তথ্যগুলো হাতে লিখে দিন");
            }

            String contentType = response.contentType();
            if (contentType != null && !contentType.toLowerCase().contains("html")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "এই লিংকটি কোনো ওয়েবপেজ নয়");
            }

            try {
                document = response.parse();
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "পেজটি পড়া গেল না");
            }
            break;
        }

        if (document == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "অনেকবার রিডাইরেক্ট হয়েছে");
        }

        return new LinkPreview(
                firstNonBlank(meta(document, "og:title"), meta(document, "twitter:title"), document.title()),
                firstNonBlank(meta(document, "og:site_name"), uri.getHost()),
                firstNonBlank(meta(document, "og:description"), meta(document, "twitter:description"),
                        meta(document, "description")),
                absoluteImage(document, uri),
                publishedOn(document));
    }

    /** http(s) only, and the host must not resolve to this machine's own networks. */
    private URI validated(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "লিংক দিন");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "লিংকটি সঠিক নয়");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "শুধু http বা https লিংক দেওয়া যাবে");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "লিংকটি সঠিক নয়");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ওয়েবসাইটটি খুঁজে পাওয়া গেল না");
        }

        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                log.warn("Blocked link preview to internal address host={} address={}", uri.getHost(), address);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "এই ঠিকানাটি ব্যবহার করা যাবে না");
            }
        }

        return uri;
    }

    private String meta(Document document, String property) {
        String value = document.select("meta[property=" + property + "]").attr("content");
        if (value.isBlank()) {
            value = document.select("meta[name=" + property + "]").attr("content");
        }
        return value;
    }

    private String absoluteImage(Document document, URI base) {
        String image = firstNonBlank(meta(document, "og:image"), meta(document, "twitter:image"));
        if (image == null) {
            return null;
        }
        try {
            return base.resolve(image.trim()).toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String publishedOn(Document document) {
        String raw = firstNonBlank(
                meta(document, "article:published_time"),
                meta(document, "datePublished"),
                document.select("time[datetime]").attr("datetime"));
        if (raw == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw).toString();
        } catch (DateTimeParseException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
