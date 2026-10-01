package com.sherrycardsshop.api.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import com.sherrycardsshop.api.auth.config.AuthProperties;
import com.sherrycardsshop.api.auth.entity.TokenAutenticacion;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite access tokens JWT de corta duración y refresh tokens opacos rotatorios.
 */
@Service
public class TokenService {

    public static final String ISSUER = "sherry-cards-shop-api";
    public static final String ROLE_CLAIM = "role";

    // Un refresh token ya rotado que vuelve a llegar dentro de este margen se trata como una
    // carrera entre pestañas; pasado el margen se considera robado y se revoca toda la sesión.
    private static final Duration REUSE_GRACE_PERIOD = Duration.ofSeconds(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final TokenAutenticacionRepository tokenRepository;
    private final AuthProperties authProperties;

    public TokenService(JwtEncoder jwtEncoder, TokenAutenticacionRepository tokenRepository, AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.tokenRepository = tokenRepository;
        this.authProperties = authProperties;
    }

    public String createAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.accessTokenTtl()))
                .subject(usuario.getId().toString())
                .claim(ROLE_CLAIM, usuario.getRol().getCode())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Transactional
    public String createRefreshToken(Usuario usuario, ClientInfo client) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenAutenticacion token = new TokenAutenticacion();
        token.setUsuario(usuario);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plus(authProperties.refreshTokenTtl()));
        token.setUserAgent(client.userAgent());
        token.setIpAddress(client.ipAddress());
        tokenRepository.save(token);
        return rawToken;
    }

    /**
     * Revoca el refresh token recibido y devuelve su usuario para emitir uno nuevo.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Usuario consumeRefreshToken(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        TokenAutenticacion token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(TokenService::invalidSession);

        if (token.getRevokedAt() != null) {
            if (token.getRevokedAt().isBefore(now.minus(REUSE_GRACE_PERIOD))) {
                tokenRepository.revokeAllActiveByUsuarioId(token.getUsuario().getId(), now);
            }
            throw invalidSession();
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw invalidSession();
        }
        token.setRevokedAt(now);
        return token.getUsuario();
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        tokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllRefreshTokens(Usuario usuario) {
        tokenRepository.revokeAllActiveByUsuarioId(usuario.getId(), LocalDateTime.now());
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }

    static ApiException invalidSession() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Sesión no válida o caducada");
    }
}
