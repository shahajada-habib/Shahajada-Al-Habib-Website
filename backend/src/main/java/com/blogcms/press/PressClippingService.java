package com.blogcms.press;

import java.util.List;

import com.blogcms.common.InputValidator;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PressClippingService {

    private final PressClippingRepository repository;
    private final LinkPreviewFetcher linkPreviewFetcher;
    private final InputValidator validator;

    public PressClippingService(
            PressClippingRepository repository,
            LinkPreviewFetcher linkPreviewFetcher,
            InputValidator validator) {
        this.repository = repository;
        this.linkPreviewFetcher = linkPreviewFetcher;
        this.validator = validator;
    }

    /** Everything the public press page shows. */
    public List<PressClipping> getPublished() {
        return repository.findByStatusOrderByPublishedOnDescIdDesc(PressClipping.ACTIVE);
    }

    /** The handful the home page teases. */
    public List<PressClipping> getPublished(int limit) {
        return repository.findByStatusOrderByPublishedOnDescIdDesc(
                PressClipping.ACTIVE, PageRequest.of(0, limit));
    }

    public boolean hasPublished() {
        return repository.countByStatus(PressClipping.ACTIVE) > 0;
    }

    /** Admin listing — hidden rows included. */
    public List<PressClipping> listAll() {
        return repository.findAllByOrderByPublishedOnDescIdDesc();
    }

    public LinkPreview preview(String url) {
        return linkPreviewFetcher.fetch(url);
    }

    @Transactional
    public PressClipping create(PressClippingDto request) {
        return save(new PressClipping(), request);
    }

    @Transactional
    public PressClipping update(Long id, PressClippingDto request) {
        PressClipping existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "clipping not found"));
        return save(existing, request);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "clipping not found");
        }
        repository.deleteById(id);
    }

    private PressClipping save(PressClipping clipping, PressClippingDto request) {
        clipping.setTitle(validator.required(request.getTitle(), "title", 300));
        clipping.setPublication(validator.required(request.getPublication(), "publication", 160));
        clipping.setUrl(externalUrl(request.getUrl()));
        clipping.setSummary(validator.optional(request.getSummary(), 2000));
        clipping.setImageUrl(validator.optional(request.getImageUrl(), 1000));
        clipping.setPublishedOn(request.getPublishedOn());
        clipping.setStatus(PressClipping.HIDDEN.equals(request.getStatus())
                ? PressClipping.HIDDEN
                : PressClipping.ACTIVE);
        return repository.save(clipping);
    }

    /**
     * Stored verbatim but scheme-checked: this URL ends up in an href, so a
     * javascript: or data: value would be a stored XSS vector.
     */
    private String externalUrl(String url) {
        String value = validator.required(url, "url", 1000);
        String lower = value.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "লিংক http:// বা https:// দিয়ে শুরু হতে হবে");
        }
        return value;
    }
}
