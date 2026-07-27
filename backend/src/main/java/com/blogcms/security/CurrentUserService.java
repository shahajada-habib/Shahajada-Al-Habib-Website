package com.blogcms.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    public String username() {
        Authentication authentication = authentication();
        return authentication.getName();
    }

    public String role() {
        Authentication authentication = authentication();

        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter((authority) -> authority.startsWith("ROLE_"))
                .map((authority) -> authority.substring(5).toLowerCase())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "role required"));
    }

    public boolean isAdmin() {
        return "admin".equals(role());
    }

    public boolean isEditor() {
        return "editor".equals(role());
    }

    public boolean isReporter() {
        return "reporter".equals(role());
    }

    public boolean canPublish() {
        String role = role();
        return "admin".equals(role) || "editor".equals(role);
    }

    public void requireAdmin(String action) {
        if (!isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin required to " + action);
        }
    }

    public void requireEditorOrAdmin(String action) {
        String role = role();
        if (!"admin".equals(role) && !"editor".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "editor or admin required to " + action);
        }
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication required");
        }
        return authentication;
    }
}
