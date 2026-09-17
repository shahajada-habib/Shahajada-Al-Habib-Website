package com.blogcms.settings;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * The author's own profile content, as a single fixed-shape row rather than a
 * key/value settings table: every field here is a known part of the public
 * site's chrome (name, bio, book, socials) that used to be hardcoded across
 * templates, not open-ended CMS content. There is exactly one row, id=1,
 * created by the V25 migration.
 */
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "author_name", length = 160)
    private String authorName;

    @Column(length = 300)
    private String tagline;

    @Column(name = "tagline_en", length = 300)
    private String taglineEn;

    @Column(name = "about_bio", columnDefinition = "TEXT")
    private String aboutBio;

    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    @Column(name = "book_title", length = 300)
    private String bookTitle;

    @Column(name = "book_year", length = 40)
    private String bookYear;

    @Column(name = "book_venue", length = 300)
    private String bookVenue;

    @Column(name = "book_cover_url", length = 1000)
    private String bookCoverUrl;

    @Column(name = "book_quote", columnDefinition = "TEXT")
    private String bookQuote;

    @Column(name = "book_quote_author", length = 300)
    private String bookQuoteAuthor;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "home_show_category_row", nullable = false)
    private boolean homeShowCategoryRow = true;

    @Column(name = "home_show_social_section", nullable = false)
    private boolean homeShowSocialSection = true;

    @Column(name = "ads_enabled", nullable = false)
    private boolean adsEnabled = false;

    @Column(name = "ad_image_url", length = 1000)
    private String adImageUrl;

    @Column(name = "ad_link_url", length = 1000)
    private String adLinkUrl;

    @Column(name = "ad_label", length = 80)
    private String adLabel;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getTaglineEn() {
        return taglineEn;
    }

    public void setTaglineEn(String taglineEn) {
        this.taglineEn = taglineEn;
    }

    public String getAboutBio() {
        return aboutBio;
    }

    public void setAboutBio(String aboutBio) {
        this.aboutBio = aboutBio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookYear() {
        return bookYear;
    }

    public void setBookYear(String bookYear) {
        this.bookYear = bookYear;
    }

    public String getBookVenue() {
        return bookVenue;
    }

    public void setBookVenue(String bookVenue) {
        this.bookVenue = bookVenue;
    }

    public String getBookCoverUrl() {
        return bookCoverUrl;
    }

    public void setBookCoverUrl(String bookCoverUrl) {
        this.bookCoverUrl = bookCoverUrl;
    }

    public String getBookQuote() {
        return bookQuote;
    }

    public void setBookQuote(String bookQuote) {
        this.bookQuote = bookQuote;
    }

    public String getBookQuoteAuthor() {
        return bookQuoteAuthor;
    }

    public void setBookQuoteAuthor(String bookQuoteAuthor) {
        this.bookQuoteAuthor = bookQuoteAuthor;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public boolean isHomeShowCategoryRow() {
        return homeShowCategoryRow;
    }

    public void setHomeShowCategoryRow(boolean homeShowCategoryRow) {
        this.homeShowCategoryRow = homeShowCategoryRow;
    }

    public boolean isHomeShowSocialSection() {
        return homeShowSocialSection;
    }

    public void setHomeShowSocialSection(boolean homeShowSocialSection) {
        this.homeShowSocialSection = homeShowSocialSection;
    }

    public boolean isAdsEnabled() {
        return adsEnabled;
    }

    public void setAdsEnabled(boolean adsEnabled) {
        this.adsEnabled = adsEnabled;
    }

    public String getAdImageUrl() {
        return adImageUrl;
    }

    public void setAdImageUrl(String adImageUrl) {
        this.adImageUrl = adImageUrl;
    }

    public String getAdLinkUrl() {
        return adLinkUrl;
    }

    public void setAdLinkUrl(String adLinkUrl) {
        this.adLinkUrl = adLinkUrl;
    }

    public String getAdLabel() {
        return adLabel;
    }

    public void setAdLabel(String adLabel) {
        this.adLabel = adLabel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
