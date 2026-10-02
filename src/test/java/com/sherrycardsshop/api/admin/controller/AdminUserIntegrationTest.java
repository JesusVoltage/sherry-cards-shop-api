package com.sherrycardsshop.api.admin.controller;

import com.jayway.jsonpath.JsonPath;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserIntegrationTest {

    private static final String PASSWORD = "una-frase-bastante-larga";

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

    private Usuario jesus;
    private Cookie admin;

    @BeforeEach
    void setUp() {
        cleanUp();
        jesus = user("jesus", Rol.ADMIN);
        admin = new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(jesus));
    }

    @AfterEach
    void cleanUp() {
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void createsUsersWithAnyRoleThatCanLogIn() throws Exception {
        mockMvc.perform(json(post("/api/admin/users"), body("brock", "Brock@Example.com", PASSWORD, "ADMIN", "ACTIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("brock@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "brock@example.com", "password": "%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void validatesNewAccounts() throws Exception {
        mockMvc.perform(json(post("/api/admin/users"), body("misty", "misty@example.com", "", "CLIENTE", "ACTIVO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value("La contraseña es obligatoria"));
        mockMvc.perform(json(post("/api/admin/users"), body("jesus", "otro@example.com", PASSWORD, "CLIENTE", "ACTIVO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.username").exists());
        mockMvc.perform(json(post("/api/admin/users"), body("misty", "misty@example.com", PASSWORD, "JEFAZO", "ACTIVO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.role").value("Rol no válido"));
    }

    @Test
    void searchesAndFiltersUsers() throws Exception {
        user("misty", Rol.CLIENTE);
        user("brock", Rol.CLIENTE);

        mockMvc.perform(get("/api/admin/users").param("search", "MIS").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("misty"));
        mockMvc.perform(get("/api/admin/users").param("role", "admin").cookie(admin))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("jesus"));
        mockMvc.perform(get("/api/admin/users/options").cookie(admin))
                .andExpect(jsonPath("$.data.roles[*].code").value(org.hamcrest.Matchers.contains("CLIENTE", "ADMIN")));
    }

    @Test
    void changingRoleOrBlockingClosesTheUsersSessions() throws Exception {
        Usuario misty = user("misty", Rol.CLIENTE);
        Cookie refresh = new Cookie(AuthCookies.REFRESH_TOKEN, tokenService.createRefreshToken(misty, new ClientInfo("test", "127.0.0.1")));

        mockMvc.perform(json(put("/api/admin/users/" + misty.getId()), body("misty", "misty@example.com", "", "CLIENTE", "BLOQUEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOQUEADO"));
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        assertThat(usuarioRepository.findById(misty.getId()).orElseThrow().getPasswordHash()).isEqualTo(misty.getPasswordHash());
    }

    @Test
    void adminsCannotLockThemselvesOut() throws Exception {
        mockMvc.perform(json(put("/api/admin/users/" + jesus.getId()), body("jesus", "jesus@example.com", "", "CLIENTE", "ACTIVO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.role").value("No puedes quitarte el rol de administrador ni bloquear tu propia cuenta"));
        mockMvc.perform(json(put("/api/admin/users/" + jesus.getId()), body("jesus", "jesus@example.com", "", "ADMIN", "BLOQUEADO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.status").exists());
        mockMvc.perform(delete("/api/admin/users/" + jesus.getId()).cookie(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesAccountsWithoutOrders() throws Exception {
        Usuario misty = user("misty", Rol.CLIENTE);
        tokenService.createRefreshToken(misty, new ClientInfo("test", "127.0.0.1"));

        mockMvc.perform(delete("/api/admin/users/" + misty.getId()).cookie(admin)).andExpect(status().isOk());
        assertThat(usuarioRepository.findById(misty.getId())).isEmpty();
    }

    @Test
    void customersCannotManageUsers() throws Exception {
        Cookie customer = new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(user("misty", Rol.CLIENTE)));
        mockMvc.perform(get("/api/admin/users").cookie(customer)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users").cookie(customer).contentType(MediaType.APPLICATION_JSON)
                        .content(body("x", "x@example.com", PASSWORD, "ADMIN", "ACTIVO")))
                .andExpect(status().isForbidden());
    }

    private Usuario user(String username, String role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(username + "@example.com");
        usuario.setUsername(username);
        usuario.setNombre(username);
        usuario.setPasswordHash("{noop}irrelevante");
        usuario.setRol(rolRepository.findByCode(role).orElseThrow());
        usuario.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static String body(String username, String email, String password, String role, String status) {
        return """
                {"username": "%s", "email": "%s", "nombre": "Nombre", "apellidos": "Apellido",
                 "password": "%s", "role": "%s", "status": "%s"}
                """.formatted(username, email, password, role, status);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.cookie(admin).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
