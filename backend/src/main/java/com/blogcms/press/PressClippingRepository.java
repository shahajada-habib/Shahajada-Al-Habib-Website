package com.blogcms.press;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PressClippingRepository extends JpaRepository<PressClipping, Long> {

    // Newest first; rows with no date sort last so an undated clipping never
    // jumps to the top of the page.
    List<PressClipping> findByStatusOrderByPublishedOnDescIdDesc(String status);

    List<PressClipping> findByStatusOrderByPublishedOnDescIdDesc(String status, Pageable pageable);

    List<PressClipping> findAllByOrderByPublishedOnDescIdDesc();

    long countByStatus(String status);

    @Query("SELECT DISTINCT p.kind FROM PressClipping p WHERE p.status = :status")
    List<String> findKindsInUse(@Param("status") String status);
}
