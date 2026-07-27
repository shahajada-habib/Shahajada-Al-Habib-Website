package com.blogcms.common;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionEnvironmentValidator implements InitializingBean {

    private static final String DEV_JWT_SECRET = "blog-cms-boilerplate-development-secret-change-before-production";
    private static final String MYSQL_DEV_JWT_SECRET = "blog-cms-boilerplate-mysql-dev-secret-change-before-production";

    private final Environment environment;
    private final Map<String, String> requiredValues;

    public ProductionEnvironmentValidator(
            Environment environment,
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.url:}") String dbUrl,
            @Value("${spring.datasource.username:}") String dbUsername,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.cors.allowed-origins:}") String corsOrigins,
            @Value("${app.media.upload-dir:}") String uploadDir,
            @Value("${app.site-url:}") String siteUrl) {
        this.environment = environment;
        this.requiredValues = new LinkedHashMap<>();
        requiredValues.put("JWT_SECRET", jwtSecret);
        requiredValues.put("DB_URL", dbUrl);
        requiredValues.put("DB_USERNAME", dbUsername);
        requiredValues.put("DB_PASSWORD", dbPassword);
        requiredValues.put("CORS_ALLOWED_ORIGINS", corsOrigins);
        requiredValues.put("MEDIA_UPLOAD_DIR", uploadDir);
        requiredValues.put("SITE_URL", siteUrl);
    }

    @Override
    public void afterPropertiesSet() {
        if (!isProd()) {
            return;
        }

        requiredValues.forEach((name, value) -> {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(name + " must be configured when SPRING_PROFILES_ACTIVE=prod");
            }
        });

        String jwtSecret = requiredValues.get("JWT_SECRET").trim();
        if (DEV_JWT_SECRET.equals(jwtSecret) || MYSQL_DEV_JWT_SECRET.equals(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be a unique production secret of at least 32 characters");
        }

        String corsOrigins = requiredValues.get("CORS_ALLOWED_ORIGINS");
        if (corsOrigins.contains("*") || corsOrigins.contains("localhost")) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must list exact production origins only");
        }

        String siteUrl = requiredValues.get("SITE_URL");
        if (!siteUrl.startsWith("https://")) {
            throw new IllegalStateException("SITE_URL must be an https origin in production");
        }
    }

    private boolean isProd() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
