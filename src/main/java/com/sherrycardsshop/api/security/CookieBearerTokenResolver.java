package com.sherrycardsshop.api.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.WebUtils;

/**
 * Lee el access token de la cabecera Authorization o, si no existe, de la cookie HttpOnly.
 * En las rutas públicas no resuelve ningún token, de modo que una cookie caducada no
 * provoca un 401 en login, refresh o en el catálogo.
 */
class CookieBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
    private final RequestMatcher publicRequests;

    CookieBearerTokenResolver(RequestMatcher publicRequests) {
        this.publicRequests = publicRequests;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (publicRequests.matches(request)) {
            return null;
        }
        String headerToken = headerResolver.resolve(request);
        if (headerToken != null) {
            return headerToken;
        }
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.ACCESS_TOKEN);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
