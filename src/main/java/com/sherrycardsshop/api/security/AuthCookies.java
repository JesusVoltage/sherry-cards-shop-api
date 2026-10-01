package com.sherrycardsshop.api.security;

import java.time.Duration;

import com.sherrycardsshop.api.auth.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookies HttpOnly de sesión. El access token viaja a toda la API; el refresh token solo a /api/auth.
 */
@Component
public class AuthCookies {

    public static final String ACCESS_TOKEN = "scs_access";
    public static final String REFRESH_TOKEN = "scs_refresh";
    private static final String ACCESS_PATH = "/";
    private static final String REFRESH_PATH = "/api/auth";

    private final AuthProperties authProperties;

    public AuthCookies(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie accessToken(String token) {
        return build(ACCESS_TOKEN, token, ACCESS_PATH, authProperties.accessTokenTtl());
    }

    public ResponseCookie refreshToken(String token) {
        return build(REFRESH_TOKEN, token, REFRESH_PATH, authProperties.refreshTokenTtl());
    }

    public ResponseCookie clearAccessToken() {
        return build(ACCESS_TOKEN, "", ACCESS_PATH, Duration.ZERO);
    }

    public ResponseCookie clearRefreshToken() {
        return build(REFRESH_TOKEN, "", REFRESH_PATH, Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        AuthProperties.Cookie cookie = authProperties.cookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(path)
                .maxAge(maxAge);
        if (cookie.domain() != null && !cookie.domain().isBlank()) {
            builder.domain(cookie.domain());
        }
        return builder.build();
    }
}
