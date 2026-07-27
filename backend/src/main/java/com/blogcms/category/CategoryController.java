package com.blogcms.category;

import java.util.List;
import java.util.Set;

import com.blogcms.security.CurrentUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "inactive");

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryController(CategoryRepository categoryRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        log.info("GET /api/categories returned {} categories", categories.size());
        return categories;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/active")
    public List<Category> getActivePublicCategories() {
        return categoryRepository.findByStatusOrderByIdAsc("active");
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        currentUserService.requireEditorOrAdmin("create category");
        category.setId(null);
        prepareForSave(category);
        rejectDuplicateSlug(category.getSlug(), null);
        return categoryRepository.save(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category request) {
        currentUserService.requireEditorOrAdmin("update category");
        return categoryRepository.findById(id)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setSlug(request.getSlug());
                    existing.setStatus(request.getStatus());
                    prepareForSave(existing);
                    rejectDuplicateSlug(existing.getSlug(), existing.getId());
                    return ResponseEntity.ok(categoryRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        currentUserService.requireAdmin("delete category");

        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void prepareForSave(Category category) {
        requireText(category.getName(), "name is required");
        requireText(category.getSlug(), "slug is required");
        category.setName(category.getName().trim());
        category.setSlug(category.getSlug().trim());
        category.setStatus(normalizeStatus(category.getStatus()));
    }

    private void rejectDuplicateSlug(String slug, Long currentCategoryId) {
        boolean duplicate = currentCategoryId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, currentCategoryId);

        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug already exists");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }

        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be active or inactive");
        }

        return normalized;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
