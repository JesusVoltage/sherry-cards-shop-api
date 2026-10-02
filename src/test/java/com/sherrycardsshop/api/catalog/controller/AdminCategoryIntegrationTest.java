package com.sherrycardsshop.api.catalog.controller;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCategoryIntegrationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private ImageStorage imageStorage;

    private final List<Long> created = new ArrayList<>();
    private Cookie admin;

    @BeforeEach
    void setUp() {
        cleanUp();
        Usuario usuario = new Usuario();
        usuario.setEmail("jesus@example.com");
        usuario.setUsername("jesus");
        usuario.setNombre("Jesús");
        usuario.setPasswordHash("{noop}irrelevante");
        usuario.setRol(rolRepository.findByCode(Rol.ADMIN).orElseThrow());
        usuario.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        admin = new Cookie(AuthCookies.ACCESS_TOKEN, tokenService.createAccessToken(usuarioRepository.saveAndFlush(usuario)));
    }

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
        // Primero las hojas: una categoría con hijas no se puede borrar.
        for (int i = created.size() - 1; i >= 0; i--) {
            categoryRepository.findById(created.get(i)).ifPresent(categoryRepository::delete);
        }
        created.clear();
        direccionRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void listsEveryCategoryIncludingTheProtectedUncategorized() throws Exception {
        mockMvc.perform(get("/api/admin/categories").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug == 'sin-categoria')].system").value(true))
                .andExpect(jsonPath("$.data[?(@.slug == 'sin-categoria')].active").value(false))
                .andExpect(jsonPath("$.data[?(@.slug == 'pokemon')].system").value(false));
    }

    @Test
    void createsNestedCategoriesAtTheEndOfTheirLevel() throws Exception {
        Long pokemon = categoryRepository.findBySlug("pokemon").orElseThrow().getId();
        Long expansions = create("Expansiones", pokemon);
        Long promos = create("Promos", pokemon);

        mockMvc.perform(get("/api/admin/categories").cookie(admin))
                .andExpect(jsonPath("$.data[?(@.id == " + expansions + ")].parentId").value(pokemon.intValue()))
                .andExpect(jsonPath("$.data[?(@.id == " + expansions + ")].displayOrder").value(0))
                .andExpect(jsonPath("$.data[?(@.id == " + promos + ")].displayOrder").value(1))
                .andExpect(jsonPath("$.data[?(@.id == " + pokemon + ")].childCount").value(2));
        assertThat(categoryRepository.findById(expansions).orElseThrow().getSlug()).isEqualTo("expansiones");
    }

    @Test
    void refusesCyclesTooManyLevelsAndNestingInsideUncategorized() throws Exception {
        Long parent = create("Nivel 1", null);
        Long child = create("Nivel 2", parent);
        mockMvc.perform(json(put("/api/admin/categories/" + parent), body("Nivel 1", child)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.parentId").value("Una categoría no puede ir dentro de sí misma ni de sus subcategorías"));

        Long deepest = child;
        for (int level = 3; level <= 6; level++) {
            deepest = create("Nivel " + level, deepest);
        }
        mockMvc.perform(json(post("/api/admin/categories"), body("Nivel 7", deepest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.parentId").value("Como máximo puede haber 6 niveles"));

        Long uncategorized = categoryRepository.findBySlug(Category.UNCATEGORIZED_SLUG).orElseThrow().getId();
        mockMvc.perform(json(post("/api/admin/categories"), body("Dentro", uncategorized)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.parentId").exists());
    }

    @Test
    void deletingMovesProductsToUncategorizedAndProtectsTheSystemCategory() throws Exception {
        Long doomed = create("Temporal", null);
        mockMvc.perform(json(post("/api/admin/products"), """
                        {"name": "Producto huérfano", "categoryId": %d, "type": "SEALED", "status": "DRAFT",
                         "variants": [{"name": "Única", "price": 5, "vatRate": 21, "stockQuantity": 1, "active": true}]}
                        """.formatted(doomed)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/categories/" + doomed).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1))
                .andExpect(jsonPath("$.message").value("Categoría borrada. 1 producto pasa a Sin categoría"));
        Long uncategorized = categoryRepository.findBySlug(Category.UNCATEGORIZED_SLUG).orElseThrow().getId();
        assertThat(productRepository.findAll()).allSatisfy(product ->
                assertThat(product.getCategory().getId()).isEqualTo(uncategorized));

        mockMvc.perform(delete("/api/admin/categories/" + uncategorized).cookie(admin)).andExpect(status().isConflict());

        Long parent = create("Con hijas", null);
        create("Hija", parent);
        mockMvc.perform(delete("/api/admin/categories/" + parent).cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tiene subcategorías: muévelas o bórralas antes"));
    }

    @Test
    void reordersOneLevel() throws Exception {
        Long parent = create("Padre", null);
        Long first = create("Primera", parent);
        Long second = create("Segunda", parent);

        mockMvc.perform(json(put("/api/admin/categories/order"), """
                        {"parentId": %d, "categoryIds": [%d, %d]}
                        """.formatted(parent, second, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + second + ")].displayOrder").value(0))
                .andExpect(jsonPath("$.data[?(@.id == " + first + ")].displayOrder").value(1));
        mockMvc.perform(json(put("/api/admin/categories/order"), """
                        {"parentId": %d, "categoryIds": [%d]}
                        """.formatted(parent, first)))
                .andExpect(status().isBadRequest());
    }

    private Long create(String name, Long parentId) throws Exception {
        String response = mockMvc.perform(json(post("/api/admin/categories"), body(name, parentId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(response, "$.data.id")).longValue();
        created.add(id);
        return id;
    }

    private static String body(String name, Long parentId) {
        return """
                {"name": "%s", "parentId": %s, "active": true}
                """.formatted(name, parentId);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.cookie(admin).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
