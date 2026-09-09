package com.blogcms.press;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PressClippingRepository extends JpaRepository<PressClipping, Long> {

    // Newest first; rows with no date sort last so an undated clipping never
    // jumps to the top of the page.
    List<PressClipping> findByStatusOrderByPublishedOnDescIdDesc(String status);

    List<PressClipping> findByStatusOrderByPublishedOnDescIdDesc(String status, Pageable pageable);

    List<PressClipping> findAllByOrderByPublishedOnDescIdDesc();

    long countByStatus(String status);
}
