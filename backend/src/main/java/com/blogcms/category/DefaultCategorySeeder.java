package com.blogcms.category;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "dev", "mysql-dev" })
public class DefaultCategorySeeder {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeeder.class);

    @Bean
    ApplicationRunner seedDefaultCategories(CategoryRepository categoryRepository) {
        return (args) -> {
            List<CategorySeed> categories = List.of(
                    new CategorySeed("কবিতা", "poetry"),
                    new CategorySeed("গল্প", "stories"),
                    new CategorySeed("জার্নাল", "journal"),
                    new CategorySeed("বুক রিভিউ", "reviews"),
                    new CategorySeed("প্রকাশিত বই", "books"),
                    new CategorySeed("গ্যালারি", "gallery"));

            int createdCount = 0;
            for (CategorySeed seed : categories) {
                if (categoryRepository.existsBySlug(seed.slug())) {
                    continue;
                }

                Category category = new Category();
                category.setName(seed.name());
                category.setSlug(seed.slug());
                category.setStatus("active");
                categoryRepository.save(category);
                createdCount++;
            }

            log.info("dev category seeder created {} categories", createdCount);
        };
    }

    private record CategorySeed(String name, String slug) {
    }
}
