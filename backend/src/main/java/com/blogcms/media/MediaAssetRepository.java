package com.blogcms.media;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    List<MediaAsset> findAllByOrderByCreatedAtDesc();

    List<MediaAsset> findByShowInGalleryTrueOrderByCreatedAtDesc();
}
