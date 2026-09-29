package com.sherrycardsshop.api.novedades.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NovedadSeedIntegrationTest {

    @Autowired
    private NovedadService novedadService;

    @Test
    void flywaySeedCreatesTheRequestedNovedadesInDisplayOrder() {
        var novedades = novedadService.getActiveNovedades();

        assertThat(novedades)
                .extracting(item -> item.title())
                .containsExactly("EB-05 de One Piece", "30 aniversario de Pokémon");
        assertThat(novedades)
                .extracting(item -> item.categorySlug())
                .containsExactly("one-piece", "pokemon");
    }
}