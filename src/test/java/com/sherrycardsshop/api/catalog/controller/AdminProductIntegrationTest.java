package com.sherrycardsshop.api.catalog.controller;

import com.jayway.jsonpath.JsonPath;
import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.catalog.repository.ProductVariantRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @MockitoBean
    private ImageStorage imageStorage;

    private Cookie admin;
    private Long pokemonId;

    @BeforeEach
    void setUp() {
        cleanUp();
        admin = accessCookie("jesus", Rol.ADMIN);
        pokemonId = categoryRepository.findAll().stream()
                .filter(category -> category.getSlug().equals("pokemon")).findFirst().orElseThrow().getId();
        when(imageStorage.enabled()).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
        mediaFileRepository.deleteAllInBatch();
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void onlyAdminsReachTheProductAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/products").cookie(accessCookie("misty", Rol.CLIENTE))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/products").cookie(admin)).andExpect(status().isOk());
    }

    @Test
    void createsAProductGeneratingSlugAndSkus() throws Exception {
        mockMvc.perform(json(post("/api/admin/products"), product("Caja de Sobres EB-05 (Español)", "ACTIVE", """
                        [{"name": "Español", "price": 99.95, "vatRate": 21, "stockQuantity": 3, "active": true},
                         {"name": "Japonés", "sku": "eb05-jp", "price": 89.00, "compareAtPrice": 95.00, "vatRate": 21,
                          "stockQuantity": 0, "weightGrams": 450, "active": true}]
                        """, """
                        [{"url": "https://cdn.test/a.webp", "altText": "Caja"}, {"url": "https://cdn.test/b.webp"}]
                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("caja-de-sobres-eb-05-espanol"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.variants[0].sku").value("CAJA-DE-SOBRES-EB-05-ESPANOL-1"))
                .andExpect(jsonPath("$.data.variants[1].sku").value("EB05-JP"))
                .andExpect(jsonPath("$.data.variants[1].compareAtPrice").value(95.00))
                .andExpect(jsonPath("$.data.images[0].altText").value("Caja"))
                .andExpect(jsonPath("$.data.images[1].url").value("https://cdn.test/b.webp"));

        // El mismo nombre recibe un slug libre en lugar de fallar.
        mockMvc.perform(json(post("/api/admin/products"), product("Caja de Sobres EB-05 (Español)", "DRAFT", oneVariant(), "[]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("caja-de-sobres-eb-05-espanol-2"));
    }

    @Test
    void listsWithSummariesAndFilters() throws Exception {
        create("Elite Trainer Box Prismatic", "ACTIVE");
        create("Fundas Dragon Shield", "DRAFT");

        mockMvc.perform(get("/api/admin/products").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].variantCount").value(1))
                .andExpect(jsonPath("$.data.items[0].minPrice").value(10.50))
                .andExpect(jsonPath("$.data.items[0].totalStock").value(4))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("Pokémon"));
        mockMvc.perform(get("/api/admin/products").param("search", "PRISMATIC").cookie(admin))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("Elite Trainer Box Prismatic"));
        mockMvc.perform(get("/api/admin/products").param("status", "draft").cookie(admin))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("DRAFT"));
    }

    @Test
    void updatesVariantsAndReleasesRemovedImages() throws Exception {
        MediaFile uploaded = new MediaFile();
        uploaded.setObjectKey("products/2026-10/old.webp");
        uploaded.setUrl("https://cdn.test/products/2026-10/old.webp");
        uploaded.setContentType("image/webp");
        uploaded.setSizeBytes(1200);
        mediaFileRepository.save(uploaded);

        String body = mockMvc.perform(json(post("/api/admin/products"), product("Booster Bundle", "DRAFT", """
                        [{"name": "Español", "price": 25, "vatRate": 21, "stockQuantity": 2, "active": true},
                         {"name": "Inglés", "price": 24, "vatRate": 21, "stockQuantity": 1, "active": true}]
                        """, """
                        [{"url": "https://cdn.test/products/2026-10/old.webp"}]
                        """)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.data.id");
        Integer spanishId = JsonPath.read(body, "$.data.variants[0].id");

        mockMvc.perform(json(put("/api/admin/products/" + id), product("Booster Bundle", "ACTIVE", """
                        [{"id": %d, "name": "Español", "price": 22.5, "vatRate": 21, "stockQuantity": 7, "active": true},
                         {"name": "Francés", "price": 23, "vatRate": 21, "stockQuantity": 0, "active": false}]
                        """.formatted(spanishId), "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants.length()").value(2))
                .andExpect(jsonPath("$.data.variants[0].id").value(spanishId))
                .andExpect(jsonPath("$.data.variants[0].price").value(22.5))
                .andExpect(jsonPath("$.data.variants[0].stockQuantity").value(7))
                .andExpect(jsonPath("$.data.variants[1].name").value("Francés"))
                .andExpect(jsonPath("$.data.images.length()").value(0));

        assertThat(variantRepository.findBySku("BOOSTER-BUNDLE-2")).isEmpty();
        verify(imageStorage).delete("products/2026-10/old.webp");
        assertThat(mediaFileRepository.findByUrl(uploaded.getUrl())).isEmpty();
    }

    @Test
    void rejectsInvalidProducts() throws Exception {
        mockMvc.perform(json(post("/api/admin/products"), """
                        {"name": " ", "categoryId": null, "type": "SEALED", "status": "DRAFT", "variants": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.categoryId").exists())
                .andExpect(jsonPath("$.data.variants").exists());
        mockMvc.perform(json(post("/api/admin/products"), product("Sin variantes activas", "ACTIVE", """
                        [{"name": "Única", "price": 5, "vatRate": 21, "stockQuantity": 1, "active": false}]
                        """, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.status").value("Un producto activo necesita al menos una variante activa"));
        mockMvc.perform(json(post("/api/admin/products"), product("Precio tachado", "DRAFT", """
                        [{"name": "Única", "price": 5, "compareAtPrice": 4, "vatRate": 21, "stockQuantity": 1, "active": true}]
                        """, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data['variants[0].compareAtPrice']").exists());

        create("Original", "DRAFT");
        mockMvc.perform(json(post("/api/admin/products"), product("Copia", "DRAFT", """
                        [{"name": "Única", "sku": "ORIGINAL-1", "price": 5, "vatRate": 21, "stockQuantity": 1, "active": true}]
                        """, "[]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data['variants[0].sku']").value("Ese SKU ya existe"));
    }

    @Test
    void deletesProductsWithoutOrders() throws Exception {
        Integer id = create("Producto de prueba", "DRAFT");

        mockMvc.perform(delete("/api/admin/products/" + id).cookie(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/products/" + id).cookie(admin)).andExpect(status().isNotFound());
    }

    @Test
    void offersCategoryPathsTypesAndStatuses() throws Exception {
        mockMvc.perform(get("/api/admin/catalog/options").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[?(@.name == 'Pokémon')].path").value("Pokémon"))
                .andExpect(jsonPath("$.data.types[0].code").value("SEALED"))
                .andExpect(jsonPath("$.data.statuses[0].code").value("DRAFT"));
    }

    @Test
    void dashboardCountsProductsAndUsers() throws Exception {
        create("Activo", "ACTIVE");
        create("Borrador", "DRAFT");

        mockMvc.perform(get("/api/admin/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products.total").value(2))
                .andExpect(jsonPath("$.data.products.active").value(1))
                .andExpect(jsonPath("$.data.products.draft").value(1))
                .andExpect(jsonPath("$.data.admins").value(1))
                .andExpect(jsonPath("$.data.storage.enabled").value(true))
                .andExpect(jsonPath("$.data.storage.limitBytes").value(9L * 1024 * 1024 * 1024));
    }

    private Integer create(String name, String status) throws Exception {
        String body = mockMvc.perform(json(post("/api/admin/products"), product(name, status, oneVariant(), "[]")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private String product(String name, String status, String variants, String images) {
        return """
                {"name": "%s", "categoryId": %d, "type": "SEALED", "status": "%s", "description": "Descripción",
                 "releaseDate": "2026-11-07", "variants": %s, "images": %s}
                """.formatted(name, pokemonId, status, variants, images);
    }

    private static String oneVariant() {
        return """
                [{"name": "Estándar", "price": 10.50, "vatRate": 21, "stockQuantity": 4, "active": true}]
                """;
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.cookie(admin).contentType(MediaType.APPLICATION_JSON).content(body);
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
