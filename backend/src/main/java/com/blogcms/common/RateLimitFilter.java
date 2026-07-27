package com.blogcms.common;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long ONE_MINUTE = 60_000L;

    private final RateLimitService rateLimitService;
    private final int loginPerMinute;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.login-per-minute:15}") int loginPerMinute) {
        this.rateLimitService = rateLimitService;
        this.loginPerMinute = loginPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        LimitRule rule = ruleFor(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getMethod() + ":" + request.getRequestURI() + ":" + clientIp(request);
        if (rateLimitService.allow(key, rule.maxRequests(), rule.windowMillis())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
    }

    private LimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(path)) {
            return new LimitRule(loginPerMinute, ONE_MINUTE);
        }

        if (HttpMethod.POST.matches(method) && path.matches("^/api/public/news/\\d+/comments$")) {
            return new LimitRule(10, ONE_MINUTE);
        }

        if (HttpMethod.PATCH.matches(method) && path.matches("^/api/news/\\d+/view$")) {
            return new LimitRule(60, ONE_MINUTE);
        }

        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record LimitRule(int maxRequests, long windowMillis) {
    }
}
