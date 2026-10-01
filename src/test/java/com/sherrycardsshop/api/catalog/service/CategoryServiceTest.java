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
        when(categoryRepository.findAllByActiveTrueAndParentIsNullOrderByDisplayOrderAsc()).thenReturn(List.of(category));

        var result = categoryService.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Pokémon");
        assertThat(result.getFirst().slug()).isEqualTo("pokemon");
        assertThat(result.getFirst().parentId()).isNull();
        verify(categoryRepository).findAllByActiveTrueAndParentIsNullOrderByDisplayOrderAsc();
    }

    @Test
    void buildsNestedTreeFromActiveCategories() {
        Category pokemon = category(1L, "Pokémon", null, 1);
        Category expansiones = category(2L, "Expansiones", pokemon, 1);
        Category japon = category(3L, "Japón", pokemon, 2);
        Category escarlata = category(4L, "Escarlata y Púrpura", expansiones, 1);
        Category onePiece = category(5L, "One Piece", null, 2);
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(pokemon, expansiones, escarlata, onePiece, japon));

        var tree = categoryService.getCategoryTree();

        assertThat(tree).extracting(node -> node.name()).containsExactly("Pokémon", "One Piece");
        assertThat(tree.getFirst().children()).extracting(node -> node.name()).containsExactly("Expansiones", "Japón");
        assertThat(tree.getFirst().children().getFirst().children()).extracting(node -> node.name())
                .containsExactly("Escarlata y Púrpura");
        assertThat(tree.get(1).children()).isEmpty();
    }

    @Test
    void hidesBranchesWhoseParentIsInactive() {
        Category pokemon = category(1L, "Pokémon", null, 1);
        Category inactiveParent = category(2L, "Oculta", pokemon, 1);
        Category orphan = category(3L, "Hija de oculta", inactiveParent, 1);
        // El repositorio solo devuelve activas: la hija aparece pero su padre no.
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(pokemon, orphan));

        var tree = categoryService.getCategoryTree();

        assertThat(tree).singleElement().satisfies(node -> assertThat(node.children()).isEmpty());
        assertThat(categoryService.getSelfAndDescendantIds(3L)).isEmpty();
    }

    @Test
    void collectsCategoryAndAllItsDescendants() {
        Category pokemon = category(1L, "Pokémon", null, 1);
        Category expansiones = category(2L, "Expansiones", pokemon, 1);
        Category escarlata = category(3L, "Escarlata y Púrpura", expansiones, 1);
        Category japon = category(4L, "Japón", pokemon, 2);
        Category onePiece = category(5L, "One Piece", null, 2);
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(pokemon, expansiones, escarlata, japon, onePiece));

        assertThat(categoryService.getSelfAndDescendantIds(1L)).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(categoryService.getSelfAndDescendantIds(2L)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(categoryService.getSelfAndDescendantIds(5L)).containsExactly(5L);
    }

    private static Category category(Long id, String name, Category parent, int displayOrder) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(name.toLowerCase().replace(' ', '-'));
        category.setParent(parent);
        category.setActive(true);
        category.setDisplayOrder(displayOrder);
        return category;
    }
}