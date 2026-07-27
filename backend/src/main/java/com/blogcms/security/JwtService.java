package com.blogcms.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String DEV_FALLBACK_SECRET = "blog-cms-boilerplate-development-secret-change-before-production";
    private static final int MIN_PROD_SECRET_LENGTH = 32;

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = resolveSecret(secret, environment).getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    private String resolveSecret(String configuredSecret, Environment environment) {
        boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch((profile) -> "prod".equalsIgnoreCase(profile));

        if (configuredSecret == null || configuredSecret.isBlank()) {
            if (prodProfile) {
                throw new IllegalStateException("app.jwt.secret must be configured for production");
            }
            return DEV_FALLBACK_SECRET;
        }

        String trimmedSecret = configuredSecret.trim();
        if (prodProfile && (trimmedSecret.length() < MIN_PROD_SECRET_LENGTH || DEV_FALLBACK_SECRET.equals(trimmedSecret))) {
            throw new IllegalStateException("app.jwt.secret must be a unique secret of at least 32 characters for production");
        }

        return trimmedSecret;
    }

    public String generateToken(String username, String role) {
        long now = Instant.now().getEpochSecond();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("iat", now);
        payload.put("exp", now + expirationSeconds);

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean validateToken(String token) {
        try {
            Map<String, Object> claims = parseClaims(token);
            return getLongClaim(claims, "exp") > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Object subject = parseClaims(token).get("sub");
        return subject == null ? "" : subject.toString();
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? "" : role.toString();
    }

    private Map<String, Object> parseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new IllegalArgumentException("invalid signature");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid claims", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("could not encode token", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("could not sign token", exception);
        }
    }

    private long getLongClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.constantTimeEquals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
