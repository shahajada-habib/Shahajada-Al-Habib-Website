package com.blogcms.news;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.blogcms.category.Category;
import com.blogcms.category.CategoryRepository;
import com.blogcms.comment.CommentRepository;
import com.blogcms.common.ContentSanitizer;
import com.blogcms.common.InputValidator;
import com.blogcms.common.PageResponse;
import com.blogcms.reaction.ReactionRepository;
import com.blogcms.reaction.ReactionService;
import com.blogcms.security.CurrentUserService;
import com.blogcms.tag.Tag;
import com.blogcms.tag.TagRepository;
import com.blogcms.user.User;
import com.blogcms.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NewsService {

    private final NewsRepository newsRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final ContentSanitizer sanitizer;
    private final InputValidator inputValidator;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public NewsService(
            NewsRepository newsRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService,
            ContentSanitizer sanitizer,
            InputValidator inputValidator,
            ReactionRepository reactionRepository,
            CommentRepository commentRepository,
            TagRepository tagRepository,
            UserRepository userRepository) {
        this.newsRepository = newsRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
        this.sanitizer = sanitizer;
        this.inputValidator = inputValidator;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    public Object getAllNews(Integer page, Integer size) {
        if (page != null || size != null) {
            Page<News> newsPage = newsRepository.findAll(createPageRequest(page, size));
            return toPageResponse(newsPage);
        }

        return newsRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    public Optional<NewsResponseDto> getNewsById(Long id) {
        return newsRepository.findById(id).map(this::toResponseDto);
    }

    public Optional<NewsResponseDto> getNewsBySlug(String slug) {
        return newsRepository.findBySlug(slug).map(this::toResponseDto);
    }

    public NewsResponseDto createNews(NewsRequestDto request) {
        News news = toEntity(request);
        news.setId(null);
        news.setViewCount(0);
        news.setCreatedBy(currentUserService.username());
        prepareForSave(news);
        ensureCanCreateStatus(news.getStatus());
        setPublishedByIfNeeded(news);
        rejectDuplicateSlug(news.getSlug(), null);
        return toResponseDto(newsRepository.save(news));
    }

    public Optional<NewsResponseDto> updateNews(Long id, NewsRequestDto request) {
        return newsRepository.findById(id)
                .map(existing -> {
                    String previousStatus = existing.getStatus();
                    ensureCanEdit(existing);
                    copyEditableFields(existing, request);
                    existing.setUpdatedBy(currentUserService.username());
                    prepareForSave(existing);
                    ensureCanSaveStatus(existing.getStatus());
                    ensureAllowedStatusTransition(previousStatus, existing.getStatus());
                    setPublishedByIfNeeded(existing);
                    rejectDuplicateSlug(existing.getSlug(), existing.getId());
                    return toResponseDto(newsRepository.save(existing));
                });
    }

    public boolean deleteNews(Long id) {
        currentUserService.requireAdmin("delete news");

        if (!newsRepository.existsById(id)) {
            return false;
        }

        // Comments and reactions hold a non-nullable FK to news with no DB-level
        // cascade, so they must be cleared explicitly before the news row goes.
        commentRepository.deleteByNewsId(id);
        reactionRepository.deleteByNewsId(id);
        newsRepository.deleteById(id);
        return true;
    }

    public Optional<NewsResponseDto> updateNewsStatus(Long id, String statusValue) {
        String status = normalizeStatus(statusValue);
        ensureCanSaveStatus(status);

        return newsRepository.findById(id)
                .map(news -> {
                    ensureCanChangeStatus(news, status);
                    news.setStatus(status);
                    news.setUpdatedBy(currentUserService.username());
                    if (NewsStatus.PUBLISHED.equals(status) && news.getPublishDate() == null) {
                        news.setPublishDate(news.getScheduledAt() == null ? LocalDateTime.now() : news.getScheduledAt());
                    }
                    setPublishedByIfNeeded(news);
                    return toResponseDto(newsRepository.save(news));
                });
    }

    public Optional<NewsResponseDto> incrementViewCount(Long id) {
        newsRepository.incrementViewCount(id);
        return newsRepository.findById(id).map(this::toResponseDto);
    }

    public Object getPublishedNews(Integer page, Integer size) {
        if (page != null || size != null) {
            PageRequest pageRequest = createPublicPageRequest(page, size);
            Page<News> newsPage = newsRepository.findVisiblePublished(NewsStatus.PUBLISHED, LocalDateTime.now(), pageRequest);
            return toPageResponse(newsPage);
        }

        return visiblePublishedNews().stream().map(this::toResponseDto).toList();
    }

    public PageResponse<NewsResponseDto> searchPublishedNews(String query, Integer page, Integer size) {
        String normalizedQuery = normalizeSearchQuery(query);
        PageRequest pageRequest = createPublicPageRequest(page, size);

        if (normalizedQuery.isBlank()) {
            Page<News> newsPage = newsRepository.findVisiblePublished(NewsStatus.PUBLISHED, LocalDateTime.now(), pageRequest);
            return toPageResponse(newsPage);
        }

        Page<News> newsPage = newsRepository.searchVisiblePublished(NewsStatus.PUBLISHED, LocalDateTime.now(), normalizedQuery, pageRequest);
        return toPageResponse(newsPage);
    }

    public Optional<NewsResponseDto> getPublishedNewsBySlug(String slug) {
        return newsRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED)
                .filter(this::isPubliclyVisible)
                .map(this::toResponseDto);
    }

    public PageResponse<NewsResponseDto> getPublishedNewsByCategory(String categorySlug, Integer page, Integer size) {
        Page<News> newsPage = newsRepository.findVisiblePublishedByCategory(
                NewsStatus.PUBLISHED,
                LocalDateTime.now(),
                categorySlug,
                createPublicPageRequest(page, size));
        return toPageResponse(newsPage);
    }

    public List<NewsResponseDto> getRelatedPublishedNews(String slug, int limit) {
        return newsRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED)
                .filter(this::isPubliclyVisible)
                .map(news -> {
                    if (news.getCategory() == null) {
                        return List.<NewsResponseDto>of();
                    }
                    return newsRepository.findRelated(
                                    NewsStatus.PUBLISHED,
                                    LocalDateTime.now(),
                                    news.getCategory().getId(),
                                    news.getId(),
                                    PageRequest.of(0, limit))
                            .stream()
                            .map(this::toResponseDto)
                            .toList();
                })
                .orElse(List.of());
    }

    public PageResponse<NewsResponseDto> getPublishedNewsByJournalist(String username, Integer page, Integer size) {
        User author = userRepository.findByUsernameAndPublicProfileTrue(username.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "journalist not found"));
        Page<News> newsPage = newsRepository.findVisiblePublishedByJournalist(
                NewsStatus.PUBLISHED,
                LocalDateTime.now(),
                author.getId(),
                author.getUsername(),
                createPublicPageRequest(page, size));
        return toPageResponse(newsPage);
    }

    private News toEntity(NewsRequestDto request) {
        News news = new News();
        copyEditableFields(news, request);
        return news;
    }

    private NewsResponseDto toResponseDto(News news) {
        return toResponseDto(news, null);
    }

    private NewsResponseDto toResponseDto(News news, Map<Long, long[]> reactionCounts) {
        NewsResponseDto response = new NewsResponseDto();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setSubtitle(news.getSubtitle());
        response.setContent(news.getContent());
        response.setImageUrl(news.getImageUrl());
        response.setImageCaption(news.getImageCaption());
        response.setImageSource(news.getImageSource());
        response.setImageAlt(news.getImageAlt());
        response.setStatus(news.getStatus());
        response.setCategory(categoryLabel(news.getCategory()));
        response.setReporterName(news.getReporterName());
        if (news.getAuthor() != null) {
            response.setAuthorUsername(news.getAuthor().getUsername());
            response.setAuthorDisplayName(displayName(news.getAuthor()));
            response.setAuthorDesignation(news.getAuthor().getDesignation());
            response.setAuthorProfileImageUrl(news.getAuthor().getProfileImageUrl());
        } else if (!isBlank(news.getReporterName())) {
            response.setAuthorDisplayName(news.getReporterName());
        }
        response.setSource(news.getSource());
        response.setVideoUrl(news.getVideoUrl());
        response.setTagNames(news.getTagEntities().stream().map(Tag::getName).sorted().toList());
        response.setSeoTitle(news.getSeoTitle());
        response.setSeoDescription(news.getSeoDescription());
        response.setSlug(news.getSlug());
        response.setBreaking(news.isBreaking());
        response.setFeatured(news.isFeatured());
        response.setScheduledAt(formatDateTime(news.getScheduledAt()));
        response.setPublishDate(formatDateTime(news.getPublishDate()));
        response.setViewCount(news.getViewCount());
        long[] counts = reactionCounts == null ? null : reactionCounts.get(news.getId());
        response.setLikeCount(counts == null ? reactionRepository.countByNewsIdAndReactionType(news.getId(), ReactionService.LIKE) : counts[0]);
        response.setDislikeCount(counts == null ? reactionRepository.countByNewsIdAndReactionType(news.getId(), ReactionService.DISLIKE) : counts[1]);
        response.setCreatedAt(news.getCreatedAt());
        response.setUpdatedAt(news.getUpdatedAt());
        return response;
    }

    private PageResponse<NewsResponseDto> toPageResponse(Page<News> page) {
        List<News> content = page.getContent();
        Map<Long, long[]> reactionCounts = reactionCounts(content);
        return new PageResponse<>(
                content.stream().map((news) -> toResponseDto(news, reactionCounts)).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast());
    }

    private Map<Long, long[]> reactionCounts(List<News> newsItems) {
        List<Long> newsIds = newsItems.stream().map(News::getId).toList();
        Map<Long, long[]> counts = new HashMap<>();
        if (newsIds.isEmpty()) {
            return counts;
        }

        for (Object[] row : reactionRepository.countByNewsIdInGrouped(newsIds)) {
            Long newsId = (Long) row[0];
            String reactionType = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] bucket = counts.computeIfAbsent(newsId, ignored -> new long[2]);
            if (ReactionService.LIKE.equals(reactionType)) {
                bucket[0] = count;
            } else if (ReactionService.DISLIKE.equals(reactionType)) {
                bucket[1] = count;
            }
        }

        return counts;
    }

    private void copyEditableFields(News target, NewsRequestDto source) {
        target.setTitle(source.getTitle());
        target.setSubtitle(source.getSubtitle());
        target.setContent(source.getContent());
        target.setImageUrl(source.getImageUrl());
        target.setImageCaption(source.getImageCaption());
        target.setImageSource(source.getImageSource());
        target.setImageAlt(source.getImageAlt());
        target.setStatus(source.getStatus());
        target.setCategory(resolveCategory(source.getCategory()));
        target.setReporterName(source.getReporterName());
        target.setAuthor(resolveAuthor(source.getAuthorId(), source.getReporterName()));
        target.setSource(source.getSource());
        target.setVideoUrl(source.getVideoUrl());
        target.setTagEntities(resolveTags(source.getTagNames()));
        target.setSeoTitle(source.getSeoTitle());
        target.setSeoDescription(source.getSeoDescription());
        target.setSlug(source.getSlug());
        target.setBreaking(source.isBreaking());
        target.setFeatured(source.isFeatured());
        target.setScheduledAt(parseDateTime(source.getScheduledAt(), "scheduledAt"));
        target.setPublishDate(parseDateTime(source.getPublishDate(), "publishDate"));
    }

    private void prepareForSave(News news) {
        requireText(news.getTitle(), "title is required");
        requireText(news.getSlug(), "slug is required");
        requireCategory(news.getCategory());
        news.setTitle(inputValidator.required(sanitizer.plainText(news.getTitle()), "title", 180));
        news.setSlug(inputValidator.slug(news.getSlug()));
        news.setSubtitle(inputValidator.optional(sanitizer.plainText(news.getSubtitle()), 300));
        news.setContent(sanitizer.articleHtml(inputValidator.optional(news.getContent(), 100000)));
        news.setImageUrl(inputValidator.optional(news.getImageUrl(), 500));
        news.setVideoUrl(inputValidator.optional(news.getVideoUrl(), 500));
        news.setImageCaption(inputValidator.optional(sanitizer.plainText(news.getImageCaption()), 500));
        news.setImageSource(inputValidator.optional(sanitizer.plainText(news.getImageSource()), 255));
        news.setImageAlt(inputValidator.optional(sanitizer.plainText(news.getImageAlt()), 255));
        news.setReporterName(inputValidator.optional(sanitizer.plainText(news.getReporterName()), 120));
        news.setSource(inputValidator.optional(sanitizer.plainText(news.getSource()), 120));
        news.setSeoTitle(inputValidator.optional(sanitizer.plainText(news.getSeoTitle()), 180));
        news.setSeoDescription(inputValidator.optional(sanitizer.plainText(news.getSeoDescription()), 300));
        news.setStatus(normalizeStatus(news.getStatus()));
        if (news.getPublishDate() == null && news.getScheduledAt() != null) {
            news.setPublishDate(news.getScheduledAt());
        }
        if (NewsStatus.PUBLISHED.equals(news.getStatus()) && news.getPublishDate() == null) {
            news.setPublishDate(LocalDateTime.now());
        }
        if (news.getViewCount() < 0) {
            news.setViewCount(0);
        }
    }

    private void rejectDuplicateSlug(String slug, Long currentNewsId) {
        boolean duplicate = currentNewsId == null
                ? newsRepository.existsBySlug(slug)
                : newsRepository.existsBySlugAndIdNot(slug, currentNewsId);

        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug already exists");
        }
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return NewsStatus.DRAFT;
        }

        String normalized = status.trim().toLowerCase();
        if (!NewsStatus.ALL.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be draft, review, published, or archived");
        }

        return normalized;
    }

    private void ensureCanSaveStatus(String status) {
        if (NewsStatus.PUBLISHED.equals(status)) {
            if (!currentUserService.canPublish()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "reporter cannot publish news");
            }
            return;
        }

        if (NewsStatus.ARCHIVED.equals(status) && currentUserService.isReporter()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "reporter cannot archive news");
        }
    }

    private void ensureCanCreateStatus(String status) {
        ensureCanSaveStatus(status);

        if (NewsStatus.ARCHIVED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new news cannot be archived");
        }
    }

    private void ensureCanChangeStatus(News news, String nextStatus) {
        String currentStatus = news.getStatus();

        if (NewsStatus.ARCHIVED.equals(nextStatus)) {
            currentUserService.requireEditorOrAdmin("archive or restore news");
            return;
        }

        if (NewsStatus.ARCHIVED.equals(currentStatus)) {
            currentUserService.requireEditorOrAdmin("archive or restore news");
            if (NewsStatus.PUBLISHED.equals(nextStatus) && !currentUserService.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restore archived news before publishing");
            }
            return;
        }

        ensureCanEdit(news);
        ensureCanSaveStatus(nextStatus);
        ensureAllowedStatusTransition(currentStatus, nextStatus);
    }

    private void ensureAllowedStatusTransition(String previousStatus, String nextStatus) {
        if (!NewsStatus.PUBLISHED.equals(nextStatus) || NewsStatus.PUBLISHED.equals(previousStatus) || currentUserService.canPublish()) {
            return;
        }

        if (!NewsStatus.REVIEW.equals(previousStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "news must be reviewed before publishing");
        }
    }

    private void ensureCanEdit(News news) {
        if (currentUserService.isAdmin()) {
            return;
        }

        if (currentUserService.isEditor()) {
            if (NewsStatus.ARCHIVED.equals(news.getStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "editor cannot edit archived news");
            }
            return;
        }

        if (currentUserService.isReporter() && currentUserService.username().equals(news.getCreatedBy())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "reporter can edit only own news");
    }

    private void setPublishedByIfNeeded(News news) {
        if (NewsStatus.PUBLISHED.equals(news.getStatus()) && isBlank(news.getPublishedBy())) {
            news.setPublishedBy(currentUserService.username());
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (isBlank(value)) {
            return null;
        }

        String normalized = value.trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be an ISO datetime");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private List<News> visiblePublishedNews() {
        return newsRepository.findVisiblePublished(NewsStatus.PUBLISHED, LocalDateTime.now());
    }

    private boolean isPubliclyVisible(News news) {
        if (!NewsStatus.PUBLISHED.equals(news.getStatus())) {
            return false;
        }

        return !publicPublishDate(news).isAfter(LocalDateTime.now());
    }

    private LocalDateTime publicPublishDate(News news) {
        if (news.getPublishDate() != null) {
            return news.getPublishDate();
        }

        if (news.getScheduledAt() != null) {
            return news.getScheduledAt();
        }

        return news.getCreatedAt() == null ? LocalDateTime.MIN : news.getCreatedAt();
    }

    private PageRequest createPageRequest(Integer page, Integer size) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(Math.max(1, size), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    private PageRequest createPublicPageRequest(Integer page, Integer size) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(Math.max(1, size), 100);
        return PageRequest.of(safePage, safeSize);
    }

    private String normalizeSearchQuery(String query) {
        if (query == null) {
            return "";
        }

        String sanitized = sanitizer.plainText(query).trim().replaceAll("\\s+", " ");
        return sanitized.substring(0, Math.min(120, sanitized.length()));
    }

    private void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void requireCategory(Category category) {
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category does not exist");
        }
    }

    private Category resolveCategory(String categoryValue) {
        requireText(categoryValue, "category is required");
        String value = categoryValue.trim();

        return categoryRepository.findBySlug(value)
                .or(() -> categoryRepository.findBySlug(value.toLowerCase()))
                .or(() -> categoryRepository.findByNameIgnoreCase(value))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "category does not exist"));
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return tags;
        }

        for (String rawName : tagNames) {
            String name = inputValidator.optional(sanitizer.plainText(rawName), 100);
            if (isBlank(name)) {
                continue;
            }

            String normalizedName = name.trim().replaceAll("\\s+", " ");
            Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> createTag(normalizedName));
            tags.add(tag);
        }

        return tags;
    }

    private User resolveAuthor(Long authorId, String reporterName) {
        if (authorId != null) {
            return userRepository.findById(authorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "author does not exist"));
        }

        if (!isBlank(reporterName)) {
            return userRepository.findByUsername(reporterName.trim().toLowerCase()).orElse(null);
        }

        return null;
    }

    private String displayName(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }

    private Tag createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(uniqueTagSlug(name));
        return tagRepository.save(tag);
    }

    private String uniqueTagSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("^-+|-+$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "tag-" + Integer.toHexString(name.hashCode()).replace("-", "");
        }

        String slug = baseSlug;
        int suffix = 2;
        while (tagRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }

        return slug;
    }

    private String categoryLabel(Category category) {
        if (category == null) {
            return "";
        }

        return category.getName() == null || category.getName().isBlank() ? category.getSlug() : category.getName();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
