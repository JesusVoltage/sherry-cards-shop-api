package com.sherrycardsshop.api.customer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DireccionUsuarioRepository direccionRepository;

    @Autowired
    private TokenAutenticacionRepository tokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/account/addresses")).andExpect(status().isUnauthorized());
    }

    @Test
    void firstAddressOfEachTypeBecomesDefaultAndANewDefaultReplacesIt() throws Exception {
        Cookie session = signUp("red");

        long home = create(session, address("Casa", true, true, false, false));
        long office = create(session, address("Oficina", true, false, true, false));

        mockMvc.perform(get("/api/account/addresses").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(home))
                .andExpect(jsonPath("$.data[0].alias").value("Casa"))
                .andExpect(jsonPath("$.data[0].predeterminadaEnvio").value(false))
                .andExpect(jsonPath("$.data[0].predeterminadaFacturacion").value(true))
                .andExpect(jsonPath("$.data[1].id").value(office))
                .andExpect(jsonPath("$.data[1].predeterminadaEnvio").value(true))
                .andExpect(jsonPath("$.data[1].complemento").doesNotExist());
    }

    @Test
    void deletingOrUnsettingTheDefaultPromotesAnotherAddress() throws Exception {
        Cookie session = signUp("blue");
        long first = create(session, address("Primera", true, false, false, false));
        long second = create(session, address("Segunda", true, false, false, false));

        mockMvc.perform(put("/api/account/addresses/{id}", first).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(address("Primera", true, false, false, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.predeterminadaEnvio").value(false));
        mockMvc.perform(get("/api/account/addresses").cookie(session))
                .andExpect(jsonPath("$.data[1].id").value(second))
                .andExpect(jsonPath("$.data[1].predeterminadaEnvio").value(true));

        mockMvc.perform(delete("/api/account/addresses/{id}", second).cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/account/addresses").cookie(session))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(first))
                .andExpect(jsonPath("$.data[0].predeterminadaEnvio").value(true));
    }

    @Test
    void usersCannotSeeOrChangeOtherUsersAddresses() throws Exception {
        long foreign = create(signUp("gold"), address("Ajena", true, true, false, false));
        Cookie session = signUp("silver");

        mockMvc.perform(get("/api/account/addresses").cookie(session))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(put("/api/account/addresses/{id}", foreign).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(address("Mía", true, false, false, false)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/account/addresses/{id}", foreign).cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesRequiredFieldsAndUsage() throws Exception {
        Cookie session = signUp("crystal");

        mockMvc.perform(post("/api/account/addresses").cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreDestinatario": " ", "telefono": "abc", "usoEnvio": false, "usoFacturacion": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.nombreDestinatario").exists())
                .andExpect(jsonPath("$.data.calle").exists())
                .andExpect(jsonPath("$.data.codigoPostal").exists())
                .andExpect(jsonPath("$.data.telefono").exists())
                .andExpect(jsonPath("$.data.uso").exists());
    }

    private Cookie signUp(String username) throws Exception {
        String email = username + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username": "%s", "email": "%s", "nombre": "Ash", "password": "secreto-seguro"}
                """.formatted(username, email))).andExpect(status().isCreated());
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "secreto-seguro"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookies.ACCESS_TOKEN);
    }

    private long create(Cookie session, String body) throws Exception {
        String response = mockMvc.perform(post("/api/account/addresses").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("id").asLong();
    }

    private static String address(String alias, boolean envio, boolean facturacion,
                                  boolean defaultEnvio, boolean defaultFacturacion) {
        return """
                {"alias": "%s", "nombreDestinatario": "Ash", "apellidosDestinatario": "Ketchum",
                 "telefono": "+34 600 000 000", "calle": "Calle Mayor", "numero": "1", "complemento": " ",
                 "codigoPostal": "28013", "localidad": "Madrid", "provincia": "Madrid", "pais": "España",
                 "usoEnvio": %s, "usoFacturacion": %s, "predeterminadaEnvio": %s, "predeterminadaFacturacion": %s}
                """.formatted(alias, envio, facturacion, defaultEnvio, defaultFacturacion);
    }
}
