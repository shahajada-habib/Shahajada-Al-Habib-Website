package com.blogcms.common;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean allow(String key, int maxRequests, long windowMillis) {
        long now = clock.millis();
        WindowCounter updated = counters.compute(key, (ignored, current) -> {
            if (current == null || now >= current.windowStartedAt + windowMillis) {
                return new WindowCounter(now, 1);
            }

            return new WindowCounter(current.windowStartedAt, current.count + 1);
        });

        return updated.count <= maxRequests;
    }

    private record WindowCounter(long windowStartedAt, int count) {
    }
}
