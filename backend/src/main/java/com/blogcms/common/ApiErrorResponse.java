package com.blogcms.common;

public record ApiErrorResponse(
        int status,
        String message,
        String path,
        String timestamp) {
}
