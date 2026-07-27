package com.blogcms.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InputValidator {

    public String required(String value, String field, int maxLength) {
        String normalized = optional(value, maxLength);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return normalized;
    }

    public String optional(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value exceeds " + maxLength + " characters");
        }
        return normalized;
    }

    public String username(String value) {
        String username = required(value, "username", 64).toLowerCase();
        if (!username.matches("^[a-z0-9._-]{3,64}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must be 3-64 lowercase letters, numbers, dots, dashes, or underscores");
        }
        return username;
    }

    public String password(String value) {
        String password = value == null ? "" : value;
        if (password.length() < 8 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be 8-128 characters");
        }
        return password;
    }

    public String slug(String value) {
        String slug = required(value, "slug", 160).toLowerCase();
        if (!slug.matches("^[a-z0-9\\u0980-\\u09ff]+(?:-[a-z0-9\\u0980-\\u09ff]+)*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug contains invalid characters");
        }
        return slug;
    }
}
