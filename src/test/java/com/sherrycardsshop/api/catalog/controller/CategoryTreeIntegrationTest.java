package com.sherrycardsshop.api.catalog.controller;

import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
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
class CategoryTreeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void exposesRootCategoriesAndTheNestedTreePublicly() throws Exception {
        Category pokemon = categoryRepository.findAll().stream()
                .filter(category -> category.getSlug().equals("pokemon")).findFirst().orElseThrow();
        Category expansiones = save("Expansiones", "pokemon-expansiones", pokemon, true);
        save("Escarlata y Púrpura", "pokemon-escarlata-purpura", expansiones, true);
        save("Oculta", "pokemon-oculta", pokemon, false);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].slug").value("pokemon"))
                .andExpect(jsonPath("$.data[0].parentId").doesNotExist());

        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("pokemon"))
                .andExpect(jsonPath("$.data[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].name").value("Expansiones"))
                .andExpect(jsonPath("$.data[0].children[0].children[0].slug").value("pokemon-escarlata-purpura"))
                .andExpect(jsonPath("$.data[1].children").isEmpty());
    }

    private Category save(String name, String slug, Category parent, boolean active) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setParent(parent);
        category.setActive(active);
        return categoryRepository.saveAndFlush(category);
    }
}
