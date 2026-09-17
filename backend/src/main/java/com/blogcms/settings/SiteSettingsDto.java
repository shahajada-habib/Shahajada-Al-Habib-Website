package com.blogcms.settings;

/** Request/response body for the admin settings form. */
public class SiteSettingsDto {

    private String authorName;
    private String tagline;
    private String taglineEn;
    private String aboutBio;
    private String profileImageUrl;
    private String bookTitle;
    private String bookYear;
    private String bookVenue;
    private String bookCoverUrl;
    private String bookQuote;
    private String bookQuoteAuthor;
    private String facebookUrl;
    private String instagramUrl;
    private String youtubeUrl;
    private boolean homeShowCategoryRow;
    private boolean homeShowSocialSection;
    private boolean adsEnabled;
    private String adImageUrl;
    private String adLinkUrl;
    private String adLabel;

    public static SiteSettingsDto from(SiteSettings entity) {
        SiteSettingsDto dto = new SiteSettingsDto();
        dto.authorName = entity.getAuthorName();
        dto.tagline = entity.getTagline();
        dto.taglineEn = entity.getTaglineEn();
        dto.aboutBio = entity.getAboutBio();
        dto.profileImageUrl = entity.getProfileImageUrl();
        dto.bookTitle = entity.getBookTitle();
        dto.bookYear = entity.getBookYear();
        dto.bookVenue = entity.getBookVenue();
        dto.bookCoverUrl = entity.getBookCoverUrl();
        dto.bookQuote = entity.getBookQuote();
        dto.bookQuoteAuthor = entity.getBookQuoteAuthor();
        dto.facebookUrl = entity.getFacebookUrl();
        dto.instagramUrl = entity.getInstagramUrl();
        dto.youtubeUrl = entity.getYoutubeUrl();
        dto.homeShowCategoryRow = entity.isHomeShowCategoryRow();
        dto.homeShowSocialSection = entity.isHomeShowSocialSection();
        dto.adsEnabled = entity.isAdsEnabled();
        dto.adImageUrl = entity.getAdImageUrl();
        dto.adLinkUrl = entity.getAdLinkUrl();
        dto.adLabel = entity.getAdLabel();
        return dto;
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
}
