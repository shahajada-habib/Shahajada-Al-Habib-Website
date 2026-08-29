package com.blogcms.cvrequest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CvRequestRepository extends JpaRepository<CvRequest, Long> {

    List<CvRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
