package com.sherrycardsshop.api.catalog.service;

import java.util.List;

import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.mapper.CategoryMapper;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
        categoryService = new CategoryService(categoryRepository, categoryMapper);
    }

    @Test
    void returnsRepositoryResultsMappedToDtos() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Pokémon");
        category.setSlug("pokemon");
        category.setActive(true);
        category.setDisplayOrder(1);
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(category));

        var result = categoryService.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Pokémon");
        assertThat(result.getFirst().slug()).isEqualTo("pokemon");
        verify(categoryRepository).findAllByActiveTrueOrderByDisplayOrderAsc();
    }
}