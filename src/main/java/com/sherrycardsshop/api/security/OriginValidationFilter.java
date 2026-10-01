package com.sherrycardsshop.api.security;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protección CSRF para una API con cookies: rechaza las peticiones que modifican estado cuando el
 * navegador declara un Origin que no es la propia API ni un origen permitido por CORS.
 */
class OriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final CorsConfigurationSource corsConfigurationSource;
    private final JsonSecurityErrorHandler errorHandler;

    OriginValidationFilter(CorsConfigurationSource corsConfigurationSource, JsonSecurityErrorHandler errorHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (SAFE_METHODS.contains(request.getMethod()) || origin == null || isAllowed(request, origin)) {
            filterChain.doFilter(request, response);
            return;
        }
        errorHandler.write(response, HttpStatus.FORBIDDEN, "Origen no permitido");
    }

    private boolean isAllowed(HttpServletRequest request, String origin) {
        if (!CorsUtils.isCorsRequest(request)) {
            return true;
        }
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);
        return configuration != null && configuration.checkOrigin(origin) != null;
    }
}
