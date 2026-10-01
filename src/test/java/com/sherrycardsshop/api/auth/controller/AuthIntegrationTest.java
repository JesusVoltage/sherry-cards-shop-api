package com.sherrycardsshop.api.auth.controller;

import java.time.LocalDateTime;

import com.sherrycardsshop.api.auth.entity.TokenAutenticacion;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.GoogleTokenVerifier;
import com.sherrycardsshop.api.auth.service.GoogleTokenVerifier.GoogleUser;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
import com.sherrycardsshop.api.security.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String PASSWORD = "secreto-seguro";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenAutenticacionRepository tokenRepository;

    @Autowired
    private DireccionUsuarioRepository direccionRepository;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void registersWithoutStartingSession() throws Exception {
        mockMvc.perform(register("ash_ketchum", "Ash@Example.com"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("ash_ketchum"))
                .andExpect(jsonPath("$.data.email").value("ash@example.com"))
                .andExpect(jsonPath("$.data.apellidos").value("Ketchum"))
                .andExpect(jsonPath("$.data.role").value("CLIENTE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVO"))
                .andExpect(jsonPath("$.data.emailVerifiedAt").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(usuarioRepository.findByEmail("ash@example.com").orElseThrow().getPasswordHash())
                .startsWith("{bcrypt}");
    }

    @Test
    void rejectsDuplicateUsernameAndEmailWithIdentifiableConflicts() throws Exception {
        mockMvc.perform(register("misty", "misty@example.com")).andExpect(status().isCreated());

        mockMvc.perform(register("MISTY", "other@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El username ya existe"));
        mockMvc.perform(register("brock", "MISTY@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    void rejectsInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "a b", "email": "no-es-email", "nombre": " ", "password": "corta"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.nombre").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void completesSessionLifecycleWithCookies() throws Exception {
        mockMvc.perform(register("gary", "gary@example.com")).andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(login("gary@example.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("gary"))
                .andExpect(jsonPath("$.data.lastAccessAt").exists())
                .andExpect(cookie().httpOnly(AuthCookies.ACCESS_TOKEN, true))
                .andExpect(cookie().httpOnly(AuthCookies.REFRESH_TOKEN, true))
                .andExpect(cookie().path(AuthCookies.REFRESH_TOKEN, "/api/auth"))
                .andReturn();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_TOKEN);
        Cookie refresh = login.getResponse().getCookie(AuthCookies.REFRESH_TOKEN);

        mockMvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("gary@example.com"));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("gary"))
                .andReturn();
        Cookie newRefresh = refreshed.getResponse().getCookie(AuthCookies.REFRESH_TOKEN);
        assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());

        // El refresh token anterior ya se rotó y no vuelve a servir.
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge(AuthCookies.REFRESH_TOKEN, 0));

        mockMvc.perform(post("/api/auth/logout").cookie(newRefresh))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(AuthCookies.ACCESS_TOKEN, 0))
                .andExpect(cookie().maxAge(AuthCookies.REFRESH_TOKEN, 0));
        mockMvc.perform(post("/api/auth/refresh").cookie(newRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokesAllSessionsWhenAnOldRefreshTokenIsReplayed() throws Exception {
        mockMvc.perform(register("jessie", "jessie@example.com")).andExpect(status().isCreated());
        Cookie stolen = mockMvc.perform(login("jessie@example.com", PASSWORD)).andReturn()
                .getResponse().getCookie(AuthCookies.REFRESH_TOKEN);
        Cookie current = mockMvc.perform(post("/api/auth/refresh").cookie(stolen)).andReturn()
                .getResponse().getCookie(AuthCookies.REFRESH_TOKEN);

        tokenRepository.findAll().stream()
                .filter(token -> token.getRevokedAt() != null)
                .forEach(token -> {
                    token.setRevokedAt(LocalDateTime.now().minusMinutes(5));
                    tokenRepository.save(token);
                });

        mockMvc.perform(post("/api/auth/refresh").cookie(stolen)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(current)).andExpect(status().isUnauthorized());
        assertThat(tokenRepository.findAll()).allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void rejectsWrongCredentialsAndLimitsAttempts() throws Exception {
        mockMvc.perform(register("james", "james@example.com")).andExpect(status().isCreated());

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(login("james@example.com", "contraseña-mala"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
        }
        mockMvc.perform(login("james@example.com", PASSWORD))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(login("nadie@example.com", PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsEndpointsWithJsonErrors() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookies.ACCESS_TOKEN, "no-es-un-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresInvalidAccessCookieOnPublicEndpoints() throws Exception {
        Cookie expired = new Cookie(AuthCookies.ACCESS_TOKEN, "caducado");
        mockMvc.perform(get("/api/categories").cookie(expired)).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").cookie(expired)).andExpect(status().isOk());
    }

    @Test
    void rejectsStateChangesFromUnknownOrigins() throws Exception {
        mockMvc.perform(register("tracey", "tracey@example.com").header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
        mockMvc.perform(register("tracey", "tracey@example.com").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void createsAccountFromGoogle() throws Exception {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleUser("google-sub-1", "Serena.Kalos@gmail.com", true, "Serena", "Kalos", "Serena Kalos"));

        mockMvc.perform(google("google-token"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookies.ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.email").value("serena.kalos@gmail.com"))
                .andExpect(jsonPath("$.data.username").value("serenakalos"))
                .andExpect(jsonPath("$.data.nombre").value("Serena"))
                .andExpect(jsonPath("$.data.apellidos").value("Kalos"))
                .andExpect(jsonPath("$.data.emailVerifiedAt").exists());

        // Un segundo login con el mismo sub reutiliza la cuenta.
        mockMvc.perform(google("google-token")).andExpect(status().isOk());
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void linksGoogleToUnverifiedAccountAndDropsItsPassword() throws Exception {
        mockMvc.perform(register("dawn", "dawn@example.com")).andExpect(status().isCreated());
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleUser("google-sub-2", "dawn@example.com", true, "Dawn", null, "Dawn"));

        mockMvc.perform(google("google-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("dawn"));

        Usuario linked = usuarioRepository.findByEmail("dawn@example.com").orElseThrow();
        assertThat(linked.getGoogleSub()).isEqualTo("google-sub-2");
        assertThat(linked.getPasswordHash()).isNull();
        assertThat(linked.getEmailVerificadoAt()).isNotNull();
        mockMvc.perform(login("dawn@example.com", PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsGoogleAccountsWithoutVerifiedEmail() throws Exception {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleUser("google-sub-3", "iris@example.com", false, "Iris", null, "Iris"));

        mockMvc.perform(google("google-token")).andExpect(status().isUnauthorized());
        assertThat(usuarioRepository.count()).isZero();
        assertThat(tokenRepository.findAll()).extracting(TokenAutenticacion::getId).isEmpty();
    }

    private static MockHttpServletRequestBuilder register(String username, String email) {
        return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username": "%s", "email": "%s", "nombre": "Ash", "apellidos": " Ketchum ", "password": "%s"}
                """.formatted(username, email, PASSWORD));
    }

    private static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    private static MockHttpServletRequestBuilder google(String credential) {
        return post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("""
                {"credential": "%s"}
                """.formatted(credential));
    }
}
