package com.sherrycardsshop.api.auth.service;

import java.util.Set;

import com.sherrycardsshop.api.auth.config.AuthProperties;
import com.sherrycardsshop.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Valida los ID tokens de Google Identity Services: firma con las claves públicas de Google,
 * emisor, audiencia (nuestro client ID) y caducidad.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final boolean enabled;
    private final JwtDecoder decoder;

    public GoogleTokenVerifier(AuthProperties authProperties) {
        this.enabled = authProperties.google().enabled();
        this.decoder = enabled ? createDecoder(authProperties.google().clientId()) : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public GoogleUser verify(String credential) {
        if (!enabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El inicio de sesión con Google no está disponible");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (JwtException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "La credencial de Google no es válida");
        }
        Object emailVerified = jwt.getClaim("email_verified");
        return new GoogleUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("name"));
    }

    private static JwtDecoder createDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> issuerAndAudience = jwt -> {
            boolean valid = ISSUERS.contains(jwt.getClaimAsString("iss"))
                    && jwt.getAudience() != null
                    && jwt.getAudience().contains(clientId);
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emisor o audiencia no válidos", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerAndAudience));
        return decoder;
    }

    public record GoogleUser(
            String subject,
            String email,
            boolean emailVerified,
            String givenName,
            String familyName,
            String name) {
    }
}
