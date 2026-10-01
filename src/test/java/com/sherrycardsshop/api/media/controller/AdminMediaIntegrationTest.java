package com.sherrycardsshop.api.media.controller;

import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
import com.sherrycardsshop.api.media.entity.MediaFile;
import com.sherrycardsshop.api.media.repository.MediaFileRepository;
import com.sherrycardsshop.api.media.storage.ImageStorage;
import com.sherrycardsshop.api.security.AuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMediaIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

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

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @MockitoBean
    private ImageStorage imageStorage;

    private Cookie admin;

    @BeforeEach
    void setUp() {
        cleanUp();
        admin = accessCookie("jesus", Rol.ADMIN);
        when(imageStorage.enabled()).thenReturn(true);
        when(imageStorage.publicUrl(anyString())).thenAnswer(call -> "https://cdn.test/" + call.getArgument(0));
    }

    @AfterEach
    void cleanUp() {
        mediaFileRepository.deleteAllInBatch();
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void uploadsImagesRecognisedByTheirContent() throws Exception {
        mockMvc.perform(multipart("/api/admin/media/images").file(file("foto.jpeg", PNG)).cookie(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.sizeBytes").value(PNG.length))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.matchesPattern(
                        "https://cdn\\.test/products/\\d{4}-\\d{2}/[0-9a-f-]{36}\\.png")));

        verify(imageStorage).put(startsWith("products/"), eq(PNG), eq("image/png"));
        assertThat(mediaFileRepository.sumSizeBytes()).isEqualTo(PNG.length);
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        mockMvc.perform(multipart("/api/admin/media/images").file(file("virus.png", "<?php echo 1;".getBytes())).cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo se admiten imágenes JPG, PNG o WebP"));
        verify(imageStorage, never()).put(anyString(), any(), anyString());
    }

    @Test
    void stopsAtTheStorageLimit() throws Exception {
        MediaFile existing = new MediaFile();
        existing.setObjectKey("products/lleno.png");
        existing.setUrl("https://cdn.test/products/lleno.png");
        existing.setContentType("image/png");
        existing.setSizeBytes(9L * 1024 * 1024 * 1024);
        mediaFileRepository.save(existing);

        mockMvc.perform(multipart("/api/admin/media/images").file(file("foto.png", PNG)).cookie(admin))
                .andExpect(status().isInsufficientStorage());
        verify(imageStorage, never()).put(anyString(), any(), anyString());
    }

    @Test
    void reportsWhenStorageIsNotConfigured() throws Exception {
        when(imageStorage.enabled()).thenReturn(false);

        mockMvc.perform(multipart("/api/admin/media/images").file(file("foto.png", PNG)).cookie(admin))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("La subida de imágenes todavía no está configurada"));
    }

    @Test
    void onlyAdminsCanUpload() throws Exception {
        mockMvc.perform(multipart("/api/admin/media/images").file(file("foto.png", PNG))).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/admin/media/images").file(file("foto.png", PNG)).cookie(accessCookie("misty", Rol.CLIENTE)))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private Cookie accessCookie(String username, String role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(username + "@example.com");
        usuario.setUsername(username);
        usuario.setNombre(username);
        usuario.setPasswordHash("{noop}irrelevante");
        usuario.setRol(rolRepository.findByCode(role).orElseThrow());
        usuario.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        return new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(usuarioRepository.saveAndFlush(usuario)));
    }
}
