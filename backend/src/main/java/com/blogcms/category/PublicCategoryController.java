package com.blogcms.category;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicCategoryController {

    private final CategoryRepository categoryRepository;

    public PublicCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/api/public/categories/active")
    public List<Category> getActiveCategories() {
        return categoryRepository.findByStatusOrderByIdAsc("active");
    }
}
