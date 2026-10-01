package com.sherrycardsshop.api.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sherrycardsshop.api.auth.config.AuthProperties;
import org.springframework.stereotype.Service;

/**
 * Limita los intentos fallidos de login por cuenta. El estado vive en memoria, por lo que se
 * pierde al reiniciar y no se comparte entre réplicas.
 */
@Service
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockout;

    public LoginAttemptService(AuthProperties authProperties) {
        this.maxFailures = authProperties.maxLoginFailures();
        this.lockout = authProperties.loginLockout();
    }

    public boolean isBlocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return false;
        }
        if (current.expired(Instant.now(), lockout)) {
            attempts.remove(key, current);
            return false;
        }
        return current.failures() >= maxFailures;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(entry -> entry.expired(now, lockout));
        }
        attempts.compute(key, (ignored, current) -> current == null || current.expired(now, lockout)
                ? new Attempts(1, now)
                : new Attempts(current.failures() + 1, current.windowStart()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private record Attempts(int failures, Instant windowStart) {

        boolean expired(Instant now, Duration lockout) {
            return windowStart.plus(lockout).isBefore(now);
        }
    }
}
