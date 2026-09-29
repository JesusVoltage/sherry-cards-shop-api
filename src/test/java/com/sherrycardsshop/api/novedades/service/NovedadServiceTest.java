package com.sherrycardsshop.api.novedades.service;

import java.util.List;

import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.novedades.entity.Novedad;
import com.sherrycardsshop.api.novedades.mapper.NovedadMapper;
import com.sherrycardsshop.api.novedades.repository.NovedadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NovedadServiceTest {

    private NovedadRepository novedadRepository;
    private NovedadService novedadService;

    @BeforeEach
    void setUp() {
        novedadRepository = mock(NovedadRepository.class);
        NovedadMapper novedadMapper = Mappers.getMapper(NovedadMapper.class);
        novedadService = new NovedadService(novedadRepository, novedadMapper);
    }

    @Test
    void returnsRepositoryResultsMappedToDtos() {
        Category category = new Category();
        category.setName("One Piece");
        category.setSlug("one-piece");

        Novedad novedad = new Novedad();
        novedad.setId(1L);
        novedad.setTitle("EB-05 de One Piece");
        novedad.setSlug("eb-05-one-piece");
        novedad.setCategory(category);
        novedad.setActive(true);
        novedad.setDisplayOrder(1);
        when(novedadRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(novedad));

        var result = novedadService.getActiveNovedades();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("EB-05 de One Piece");
        assertThat(result.getFirst().categoryName()).isEqualTo("One Piece");
        assertThat(result.getFirst().categorySlug()).isEqualTo("one-piece");
        verify(novedadRepository).findAllByActiveTrueOrderByDisplayOrderAsc();
    }
}