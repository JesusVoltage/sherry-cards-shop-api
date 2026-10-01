package com.sherrycardsshop.api.auth.controller;

import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.ClientInfo;
import com.sherrycardsshop.api.auth.service.TokenService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.site.closed=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteClosedIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void rejectsRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "ash", "email": "ash@example.com", "nombre": "Ash", "password": "%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("La tienda todavía no está abierta"));
    }

    @Test
    void onlyAdminsCanLogIn() throws Exception {
        user("misty", Rol.CLIENTE);
        user("jesus", Rol.ADMIN);

        mockMvc.perform(login("misty@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.message").value("La tienda todavía no está abierta"));

        MvcResult admin = mockMvc.perform(login("jesus@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(Rol.ADMIN))
                .andReturn();
        Cookie access = admin.getResponse().getCookie(AuthCookies.ACCESS_TOKEN);

        mockMvc.perform(get("/api/categories/tree").cookie(access)).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());
    }

    @Test
    void catalogIsNoLongerPublic() throws Exception {
        mockMvc.perform(get("/api/categories/tree")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/novedades")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void existingCustomerSessionsStopWorking() throws Exception {
        Usuario customer = user("brock", Rol.CLIENTE);
        Cookie access = new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(customer));
        Cookie refresh = new Cookie(AuthCookies.REFRESH_TOKEN,
                tokenService.createRefreshToken(customer, new ClientInfo("test", "127.0.0.1")));

        mockMvc.perform(get("/api/categories/tree").cookie(access)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/account/addresses").cookie(access)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isForbidden())
                .andExpect(cookie().maxAge(AuthCookies.REFRESH_TOKEN, 0));
    }

    private Usuario user(String username, String role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(username + "@example.com");
        usuario.setUsername(username);
        usuario.setNombre(username);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByCode(role).orElseThrow());
        usuario.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static MockHttpServletRequestBuilder login(String email) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD));
    }
}
