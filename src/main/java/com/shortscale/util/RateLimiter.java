package com.shortscale.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class RateLimiter {
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();
    private final int maxRequests = 10; // per minute
    private final long windowSeconds = 60;
    private final Supplier<LocalDateTime> currentTime;

    public RateLimiter() {
        this(LocalDateTime::now);
    }

    public RateLimiter(Supplier<LocalDateTime> currentTime) {
        this.currentTime = currentTime;
    }

    public boolean isAllowed(String key) {
        LocalDateTime now = currentTime.get();
        RequestInfo info = requestCounts.get(key);
        if (info == null) {
            requestCounts.put(key, new RequestInfo(1, now));
            return true;
        }
        long secondsSince = ChronoUnit.SECONDS.between(info.timestamp, now);
        if (secondsSince > windowSeconds) {
            requestCounts.put(key, new RequestInfo(1, now));
            return true;
        }
        if (info.count >= maxRequests) {
            return false;
        }
        info.count++;
        return true;
    }

    private static class RequestInfo {
        int count;
        LocalDateTime timestamp;

        RequestInfo(int count, LocalDateTime timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
