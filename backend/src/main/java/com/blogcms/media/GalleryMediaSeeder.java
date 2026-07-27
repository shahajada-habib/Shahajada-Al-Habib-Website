package com.blogcms.media;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "dev", "mysql-dev" })
public class GalleryMediaSeeder {

    private static final Logger log = LoggerFactory.getLogger(GalleryMediaSeeder.class);

    @Bean
    ApplicationRunner seedGalleryPhotos(MediaAssetRepository mediaAssetRepository) {
        return (args) -> {
            List<PhotoSeed> photos = List.of(
                    new PhotoSeed("/assets/gallery/boimela1.jpg", "বইমেলা ১"),
                    new PhotoSeed("/assets/gallery/boimela2.jpg", "বইমেলা ২"),
                    new PhotoSeed("/assets/gallery/boimela3.jpg", "বইমেলা ৩"),
                    new PhotoSeed("/assets/gallery/travel1.jpg", "ভ্রমণ ১"),
                    new PhotoSeed("/assets/gallery/travel2.jpg", "ভ্রমণ ২"),
                    new PhotoSeed("/assets/gallery/travel3.jpg", "ভ্রমণ ৩"));

            int createdCount = 0;
            for (PhotoSeed seed : photos) {
                if (!mediaAssetRepository.findAllByOrderByCreatedAtDesc().stream()
                        .anyMatch(asset -> seed.fileUrl().equals(asset.getFileUrl()))) {
                    MediaAsset asset = new MediaAsset();
                    asset.setFileName(seed.caption());
                    asset.setFileUrl(seed.fileUrl());
                    asset.setContentType("image/jpeg");
                    asset.setSize(0);
                    asset.setUploadedBy("admin");
                    asset.setCaption(seed.caption());
                    asset.setShowInGallery(true);
                    mediaAssetRepository.save(asset);
                    createdCount++;
                }
            }

            log.info("dev gallery seeder created {} photos", createdCount);
        };
    }

    private record PhotoSeed(String fileUrl, String caption) {
    }
}
