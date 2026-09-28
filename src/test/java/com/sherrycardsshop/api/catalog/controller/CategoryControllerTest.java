package com.sherrycardsshop.api.catalog.controller;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService)).build();
    }

    @Test
    void returnsCategoriesInApiResponseEnvelope() throws Exception {
        when(categoryService.getActiveCategories()).thenReturn(List.of(
                new CategoryDto(1L, "Pokémon", "pokemon", null, null, 1)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Pokémon"))
                .andExpect(jsonPath("$.data[0].slug").value("pokemon"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data[0].active").doesNotExist());
    }
}