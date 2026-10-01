package com.sherrycardsshop.api.customer.controller;

import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.BreachedPasswordChecker;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
import com.sherrycardsshop.api.security.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrationTest {

    private static final String PASSWORD = "secreto-seguro";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenAutenticacionRepository tokenRepository;

    @Autowired
    private DireccionUsuarioRepository direccionRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EstadoUsuarioRepository estadoRepository;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private BreachedPasswordChecker breachedPasswordChecker;

    @BeforeEach
    void setUp() {
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void updatesProfileButNotEmail() throws Exception {
        Session session = signUp("brock");

        mockMvc.perform(put("/api/account/profile").cookie(session.access()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "Brock_Pewter", "nombre": " Brock ", "apellidos": " ", "email": "otro@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("Brock_Pewter"))
                .andExpect(jsonPath("$.data.nombre").value("Brock"))
                .andExpect(jsonPath("$.data.apellidos").doesNotExist())
                .andExpect(jsonPath("$.data.email").value("brock@example.com"))
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.googleLinked").value(false));
    }

    @Test
    void rejectsTakenUsernameWithFieldError() throws Exception {
        signUp("misty");
        Session session = signUp("tracey");

        mockMvc.perform(put("/api/account/profile").cookie(session.access()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "MISTY", "nombre": "Tracey"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.username").value("El username ya existe"));
    }

    @Test
    void changesPasswordAndClosesOtherSessions() throws Exception {
        Session phone = signUp("dawn");
        Session laptop = login("dawn@example.com", PASSWORD);

        MvcResult result = mockMvc.perform(changePassword(laptop, PASSWORD, "nueva-frase-larga"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookies.ACCESS_TOKEN))
                .andExpect(cookie().exists(AuthCookies.REFRESH_TOKEN))
                .andReturn();
        Cookie renewed = result.getResponse().getCookie(AuthCookies.REFRESH_TOKEN);

        mockMvc.perform(post("/api/auth/refresh").cookie(phone.refresh())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(renewed)).andExpect(status().isOk());
        mockMvc.perform(loginRequest("dawn@example.com", PASSWORD)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginRequest("dawn@example.com", "nueva-frase-larga")).andExpect(status().isOk());
    }

    @Test
    void validatesCurrentAndNewPassword() throws Exception {
        Session session = signUp("iris");

        mockMvc.perform(changePassword(session, "incorrecta", "nueva-frase-larga"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.currentPassword").value("La contraseña actual no es correcta"));
        mockMvc.perform(changePassword(session, PASSWORD, PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").exists());
        mockMvc.perform(changePassword(session, PASSWORD, "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").value("La contraseña debe tener al menos 8 caracteres"));
        mockMvc.perform(changePassword(session, PASSWORD, "soy-iris-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").value("La contraseña no puede contener tu nombre de usuario ni tu correo"));

        when(breachedPasswordChecker.isBreached("qwertyuiop123")).thenReturn(true);
        mockMvc.perform(changePassword(session, PASSWORD, "qwertyuiop123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").value("Esta contraseña aparece en filtraciones de datos conocidas. Elige otra distinta"));
    }

    @Test
    void limitsWrongCurrentPasswordAttempts() throws Exception {
        Session session = signUp("cilan");
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(changePassword(session, "incorrecta", "nueva-frase-larga")).andExpect(status().isBadRequest());
        }
        mockMvc.perform(changePassword(session, PASSWORD, "nueva-frase-larga")).andExpect(status().isTooManyRequests());
    }

    @Test
    void googleOnlyAccountsCanCreateAPasswordWithoutCurrentOne() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("serena@example.com");
        usuario.setUsername("serena");
        usuario.setNombre("Serena");
        usuario.setGoogleSub("google-sub");
        usuario.setRol(rolRepository.findByCode("CLIENTE").orElseThrow());
        usuario.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        usuario = usuarioRepository.saveAndFlush(usuario);
        Cookie access = new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(usuario));

        mockMvc.perform(put("/api/account/password").cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword": "una-frase-propia"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.googleLinked").value(true));
        mockMvc.perform(loginRequest("serena@example.com", "una-frase-propia")).andExpect(status().isOk());
    }

    private Session signUp(String username) throws Exception {
        String email = username + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username": "%s", "email": "%s", "nombre": "Ash", "password": "%s"}
                """.formatted(username, email, PASSWORD))).andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    private Session login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(email, password)).andExpect(status().isOk()).andReturn();
        return new Session(result.getResponse().getCookie(AuthCookies.ACCESS_TOKEN),
                result.getResponse().getCookie(AuthCookies.REFRESH_TOKEN));
    }

    private static MockHttpServletRequestBuilder loginRequest(String email, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    private static MockHttpServletRequestBuilder changePassword(Session session, String current, String next) {
        return put("/api/account/password").cookie(session.access()).contentType(MediaType.APPLICATION_JSON).content("""
                {"currentPassword": "%s", "newPassword": "%s"}
                """.formatted(current, next));
    }

    private record Session(Cookie access, Cookie refresh) {
    }
}
