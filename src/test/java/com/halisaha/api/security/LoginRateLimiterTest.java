package com.halisaha.api.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.halisaha.api.exception.TooManyRequestsException;

class LoginRateLimiterTest {

    private final LoginRateLimiter limiter = new LoginRateLimiter(3, 15);

    @Test
    void checkAllowed_doesNothing_whenNoFailuresRecorded() {
        assertThatCode(() -> limiter.checkAllowed("fresh@example.com")).doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_blocks_afterReachingMaxAttempts() {
        limiter.recordFailure("victim@example.com");
        limiter.recordFailure("victim@example.com");
        limiter.recordFailure("victim@example.com");

        assertThatThrownBy(() -> limiter.checkAllowed("victim@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void checkAllowed_stillAllows_belowMaxAttempts() {
        limiter.recordFailure("almost@example.com");
        limiter.recordFailure("almost@example.com");

        assertThatCode(() -> limiter.checkAllowed("almost@example.com")).doesNotThrowAnyException();
    }

    @Test
    void recordSuccess_clearsFailureHistory() {
        limiter.recordFailure("comeback@example.com");
        limiter.recordFailure("comeback@example.com");
        limiter.recordFailure("comeback@example.com");

        limiter.recordSuccess("comeback@example.com");

        assertThatCode(() -> limiter.checkAllowed("comeback@example.com")).doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_isCaseInsensitiveOnEmail() {
        limiter.recordFailure("Mixed@Example.com");
        limiter.recordFailure("mixed@example.com");
        limiter.recordFailure("MIXED@EXAMPLE.COM");

        assertThatThrownBy(() -> limiter.checkAllowed("mixed@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void checkAllowed_allowsAgain_afterWindowExpires() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        java.util.concurrent.atomic.AtomicReference<Instant> now = new java.util.concurrent.atomic.AtomicReference<>(start);
        limiter.setClock(new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        });

        limiter.recordFailure("timeout@example.com");
        limiter.recordFailure("timeout@example.com");
        limiter.recordFailure("timeout@example.com");
        assertThatThrownBy(() -> limiter.checkAllowed("timeout@example.com"))
                .isInstanceOf(TooManyRequestsException.class);

        now.set(start.plusSeconds(16 * 60));

        assertThatCode(() -> limiter.checkAllowed("timeout@example.com")).doesNotThrowAnyException();
    }
}
