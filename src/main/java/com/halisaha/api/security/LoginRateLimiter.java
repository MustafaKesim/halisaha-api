package com.halisaha.api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.halisaha.api.exception.TooManyRequestsException;

// Ayni email icin kisa surede cok fazla basarisiz giris denemesi olursa
// hesabi gecici olarak bloke eder (brute-force korumasi). Tek instance
// calistigi icin in-memory yeterli; Redis gibi paylasimli bir store gerekmiyor.
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private Clock clock = Clock.systemUTC();

    public LoginRateLimiter(
            @Value("${app.rate-limit.login.max-attempts:5}") int maxAttempts,
            @Value("${app.rate-limit.login.window-minutes:15}") long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private record Attempt(int failureCount, Instant windowStart) {}

    public void checkAllowed(String email) {
        Attempt attempt = attempts.get(normalize(email));
        if (attempt == null || isExpired(attempt)) {
            return;
        }

        if (attempt.failureCount() >= maxAttempts) {
            long remainingMinutes = Math.max(1,
                    Duration.between(clock.instant(), attempt.windowStart().plus(window)).toMinutes() + 1);
            throw new TooManyRequestsException(
                    "Çok fazla başarısız giriş denemesi. Lütfen " + remainingMinutes + " dakika sonra tekrar dene.");
        }
    }

    public void recordFailure(String email) {
        attempts.compute(normalize(email), (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Attempt(1, clock.instant());
            }
            return new Attempt(existing.failureCount() + 1, existing.windowStart());
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(normalize(email));
    }

    private boolean isExpired(Attempt attempt) {
        return clock.instant().isAfter(attempt.windowStart().plus(window));
    }

    // Locale.ROOT sart: Turkce locale'de "I".toLowerCase() -> "ı" (noktasiz) donuyor,
    // bu da ayni email'in buyuk/kucuk harf varyasyonlarinin farkli anahtarlara
    // dusmesine yol acardi.
    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
