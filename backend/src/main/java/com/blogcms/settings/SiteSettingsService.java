package com.blogcms.settings;

import com.blogcms.common.ContentSanitizer;
import com.blogcms.common.InputValidator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SiteSettingsService {

    private final SiteSettingsRepository repository;
    private final ContentSanitizer sanitizer;
    private final InputValidator validator;

    public SiteSettingsService(
            SiteSettingsRepository repository,
            ContentSanitizer sanitizer,
            InputValidator validator) {
        this.repository = repository;
        this.sanitizer = sanitizer;
        this.validator = validator;
    }

    /**
     * The one row. V25 seeds it via SQL for Flyway-managed databases (mysql-dev,
     * prod); the dev profile's in-memory H2 has Hibernate create the table from
     * the entity but never runs that migration, so this creates and persists a
     * default row the first time anything asks — every field non-blank, so a
     * public template never hits a null on {@code ${siteSettings.authorName}}.
     */
    public SiteSettings get() {
        return repository.findById(SiteSettings.SINGLETON_ID).orElseGet(this::createDefault);
    }

    private SiteSettings createDefault() {
        SiteSettings settings = new SiteSettings();
        settings.setAuthorName("শাহজাদা আল হাবীব");
        settings.setTagline("লেখক • কবি • ভ্রমণপিপাসু • সাইক্লিস্ট • ফটোগ্রাফার");
        settings.setTaglineEn("Writer • Poet • Traveler • Cyclist • Photographer");
        settings.setAboutBio("");
        settings.setProfileImageUrl("/assets/profile-440.jpg");
        settings.setBookTitle("");
        settings.setBookYear("");
        settings.setBookVenue("");
        settings.setBookCoverUrl("");
        settings.setBookQuote("");
        settings.setBookQuoteAuthor("");
        settings.setFacebookUrl("");
        settings.setInstagramUrl("");
        settings.setYoutubeUrl("");
        return repository.save(settings);
    }

    @Transactional
    public SiteSettings update(SiteSettingsDto request) {
        SiteSettings settings = repository.findById(SiteSettings.SINGLETON_ID).orElseGet(SiteSettings::new);

        settings.setAuthorName(validator.required(request.getAuthorName(), "authorName", 160));
        settings.setTagline(validator.optional(request.getTagline(), 300));
        settings.setTaglineEn(validator.optional(request.getTaglineEn(), 300));
        // Rich text from Quill, same treatment as article content.
        settings.setAboutBio(sanitizer.articleHtml(request.getAboutBio()));
        settings.setProfileImageUrl(optionalUrl(request.getProfileImageUrl(), 1000));

        settings.setBookTitle(validator.optional(request.getBookTitle(), 300));
        settings.setBookYear(validator.optional(request.getBookYear(), 40));
        settings.setBookVenue(validator.optional(request.getBookVenue(), 300));
        settings.setBookCoverUrl(optionalUrl(request.getBookCoverUrl(), 1000));
        // Plain text, not rich HTML: a one-line quote is typed into a plain textarea,
        // not the Quill editor, so there is no tag markup to preserve here.
        settings.setBookQuote(sanitizer.plainText(request.getBookQuote()));
        settings.setBookQuoteAuthor(validator.optional(request.getBookQuoteAuthor(), 300));

        settings.setFacebookUrl(optionalUrl(request.getFacebookUrl(), 500));
        settings.setInstagramUrl(optionalUrl(request.getInstagramUrl(), 500));
        settings.setYoutubeUrl(optionalUrl(request.getYoutubeUrl(), 500));

        settings.setHomeShowCategoryRow(request.isHomeShowCategoryRow());
        settings.setHomeShowSocialSection(request.isHomeShowSocialSection());
        // An ad can't actually show without an image regardless of the switch,
        // but the switch is still saved as-is so a paused campaign resumes
        // with one click once the image comes back.
        settings.setAdsEnabled(request.isAdsEnabled());
        settings.setAdImageUrl(optionalUrl(request.getAdImageUrl(), 1000));
        settings.setAdLinkUrl(optionalUrl(request.getAdLinkUrl(), 1000));
        settings.setAdLabel(validator.optional(request.getAdLabel(), 80));

        return repository.save(settings);
    }

    /**
     * Image and social links render straight into src/href attributes, so a
     * javascript: or data: value would be stored XSS. A bare site-relative
     * path (the built-in /assets/... images) is allowed through unchanged.
     */
    private String optionalUrl(String value, int maxLength) {
        String normalized = validator.optional(value, maxLength);
        if (normalized.isBlank()) {
            return "";
        }
        String lower = normalized.toLowerCase();
        boolean safe = normalized.startsWith("/") || lower.startsWith("http://") || lower.startsWith("https://");
        if (!safe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "লিংক / দিয়ে বা http(s):// দিয়ে শুরু হতে হবে");
        }
        return normalized;
    }
}
