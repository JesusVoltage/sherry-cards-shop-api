package com.sherrycardsshop.api.novedades.controller;

import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.novedades.entity.Novedad;
import com.sherrycardsshop.api.novedades.repository.NovedadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NovedadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NovedadRepository novedadRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        novedadRepository.deleteAllInBatch();
    }

    @Test
    void returnsOnlyActiveNovedadesInDisplayOrderWithCategoryDetails() throws Exception {
        Category category = createCategory();
        createNovedad("Segunda novedad", "second-novedades", true, 20, category);
        createNovedad("Novedad inactiva", "inactive-novedades", false, 0, category);
        createNovedad("Primera novedad", "first-novedades", true, 10, category);

        mockMvc.perform(get("/api/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Primera novedad"))
                .andExpect(jsonPath("$.data[0].slug").value("first-novedades"))
                .andExpect(jsonPath("$.data[0].description").value("Descripción de prueba"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/novedades.jpg"))
                .andExpect(jsonPath("$.data[0].categoryName").value("Categoría de prueba"))
                .andExpect(jsonPath("$.data[0].categorySlug").value("novedades-test-category"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(10))
                .andExpect(jsonPath("$.data[0].active").doesNotExist())
                .andExpect(jsonPath("$.data[0].category").doesNotExist())
                .andExpect(jsonPath("$.data[1].slug").value("second-novedades"))
                .andExpect(jsonPath("$.data[1].displayOrder").value(20));
    }

    @Test
    void returnsEmptyListWhenNoNovedadesAreActive() throws Exception {
        createNovedad("Novedad inactiva", "inactive-novedades", false, 0, createCategory());

        mockMvc.perform(get("/api/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private Category createCategory() {
        Category category = new Category();
        category.setName("Categoría de prueba");
        category.setSlug("novedades-test-category");
        category.setActive(true);
        return categoryRepository.saveAndFlush(category);
    }

    private void createNovedad(String title, String slug, boolean active, int displayOrder, Category category) {
        Novedad novedad = new Novedad();
        novedad.setTitle(title);
        novedad.setSlug(slug);
        novedad.setDescription("Descripción de prueba");
        novedad.setImageUrl("https://example.com/novedades.jpg");
        novedad.setCategory(category);
        novedad.setActive(active);
        novedad.setDisplayOrder(displayOrder);
        novedadRepository.saveAndFlush(novedad);
    }
}
