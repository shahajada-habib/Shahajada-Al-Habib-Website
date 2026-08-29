package com.blogcms.cvrequest;

import java.time.LocalDateTime;

public record CvRequestResponseDto(
        Long id,
        String name,
        String email,
        String purpose,
        String status,
        LocalDateTime createdAt,
        LocalDateTime handledAt) {

    static CvRequestResponseDto from(CvRequest request) {
        return new CvRequestResponseDto(
                request.getId(),
                request.getName(),
                request.getEmail(),
                request.getPurpose(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getHandledAt());
    }
}
