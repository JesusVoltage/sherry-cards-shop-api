package com.sherrycardsshop.api.novedades.controller;

import java.util.List;

import com.sherrycardsshop.api.novedades.dto.NovedadDto;
import com.sherrycardsshop.api.novedades.service.NovedadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NovedadControllerTest {

    private NovedadService novedadService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        novedadService = mock(NovedadService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new NovedadController(novedadService)).build();
    }

    @Test
    void returnsNovedadesInApiResponseEnvelope() throws Exception {
        when(novedadService.getActiveNovedades()).thenReturn(List.of(
                new NovedadDto(1L, "EB-05 de One Piece", "eb-05-one-piece", "Novedad del set EB-05 de One Piece.",
                        null, "One Piece", "one-piece", 1)));

        mockMvc.perform(get("/api/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("EB-05 de One Piece"))
                .andExpect(jsonPath("$.data[0].categorySlug").value("one-piece"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1));
    }
}