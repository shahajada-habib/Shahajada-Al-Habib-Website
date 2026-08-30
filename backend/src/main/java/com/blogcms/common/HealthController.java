package com.blogcms.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Cheap liveness probe. The {@code SELECT 1} is deliberate: an external
     * keep-alive pinger hits this every few minutes so the free-tier managed
     * database never idles into a power-off (from which it does not wake on its
     * own). Returns 503 if the database round-trip fails.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "UP", "db", "UP"));
        } catch (RuntimeException ex) {
            log.warn("Health check DB probe failed: {}", ex.getMessage());
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "db", "DOWN"));
        }
    }
}
