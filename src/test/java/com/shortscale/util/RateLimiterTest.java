package com.shortscale.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {

    @Test
    public void shouldAllowFirstRequest() {
        RateLimiter rateLimiter = new RateLimiter();
        assertTrue(rateLimiter.isAllowed("ip1"));
    }

    @Test
    public void shouldAllowRequestsWithinLimit() {
        RateLimiter rateLimiter = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.isAllowed("ip1"));
        }
    }

    @Test
    public void shouldDenyRequestWhenLimitExceeded() {
        RateLimiter rateLimiter = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            rateLimiter.isAllowed("ip1");
        }
        assertFalse(rateLimiter.isAllowed("ip1"));
    }

    @Test
    public void shouldAllowRequestAfterTimeWindowResets() {
        AtomicReference<LocalDateTime> currentTime = new AtomicReference<>(LocalDateTime.of(2023, 1, 1, 12, 0, 0));
        Supplier<LocalDateTime> timeSupplier = currentTime::get;
        RateLimiter rateLimiter = new RateLimiter(timeSupplier);

        // Make 10 requests
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.isAllowed("ip1"));
        }
        // 11th should be false
        assertFalse(rateLimiter.isAllowed("ip1"));

        // Advance time by more than 60 seconds
        currentTime.set(currentTime.get().plusSeconds(61));

        // Now should be allowed again, and can do 10 more
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.isAllowed("ip1"));
        }
        assertFalse(rateLimiter.isAllowed("ip1"));
    }

    @Test
    public void shouldAllowRequestsForDifferentKeysIndependently() {
        RateLimiter rateLimiter = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.isAllowed("ip1"));
            assertTrue(rateLimiter.isAllowed("ip2"));
        }
        assertFalse(rateLimiter.isAllowed("ip1"));
        assertFalse(rateLimiter.isAllowed("ip2"));
    }

    // Note: Time-based tests are hard without mocking time, but the above covers the main logic.
}
