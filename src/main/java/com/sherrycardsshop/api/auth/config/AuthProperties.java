package com.sherrycardsshop.api.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        int maxLoginFailures,
        Duration loginLockout,
        boolean breachedPasswordCheck,
        Cookie cookie,
        Google google) {

    private static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET debe tener al menos 32 bytes");
        }
        if ("None".equalsIgnoreCase(cookie.sameSite()) && !cookie.secure()) {
            throw new IllegalStateException("Las cookies SameSite=None requieren APP_AUTH_COOKIE_SECURE=true");
        }
    }

    public record Cookie(boolean secure, String sameSite, String domain) {
    }

    public record Google(String clientId) {

        public boolean enabled() {
            return clientId != null && !clientId.isBlank();
        }
    }
}
