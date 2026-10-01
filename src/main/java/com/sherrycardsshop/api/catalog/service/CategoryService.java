package com.sherrycardsshop.api.catalog.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.dto.CategoryTreeDto;
import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.mapper.CategoryMapper;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Categorías en árbol (lista de adyacencia). El catálogo de categorías es pequeño, así que el
 * árbol se monta en memoria a partir de una sola consulta. Solo se recorre desde las raíces, por
 * lo que una rama con un padre inactivo queda oculta entera y un ciclo nunca se alcanza.
 */
@Service
public class CategoryService {

    static final int MAX_DEPTH = 6;

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /** Categorías raíz activas (Pokémon, One Piece…), en orden de presentación. */
    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryMapper.toDtoList(categoryRepository.findAllByActiveTrueAndParentIsNullOrderByDisplayOrderAsc());
    }

    /** Árbol completo de categorías activas. */
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> getCategoryTree() {
        Map<Long, List<Category>> childrenByParent = activeChildrenByParent();
        return buildLevel(childrenByParent.getOrDefault(null, List.of()), childrenByParent, 1);
    }

    /**
     * Ids de la categoría y de todas sus subcategorías activas, para listar los productos de una
     * categoría incluyendo los de sus hijas. Vacío si la categoría no está en el árbol visible.
     */
    @Transactional(readOnly = true)
    public Set<Long> getSelfAndDescendantIds(Long categoryId) {
        Map<Long, List<Category>> childrenByParent = activeChildrenByParent();
        Set<Long> ids = new LinkedHashSet<>();
        collectVisible(childrenByParent.getOrDefault(null, List.of()), childrenByParent, categoryId, false, ids, 1);
        return ids;
    }

    private Map<Long, List<Category>> activeChildrenByParent() {
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        for (Category category : categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()) {
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }
        return childrenByParent;
    }

    private List<CategoryTreeDto> buildLevel(List<Category> categories, Map<Long, List<Category>> childrenByParent, int depth) {
        List<CategoryTreeDto> level = new ArrayList<>();
        for (Category category : categories) {
            List<CategoryTreeDto> children = depth >= MAX_DEPTH ? List.of()
                    : buildLevel(childrenByParent.getOrDefault(category.getId(), List.of()), childrenByParent, depth + 1);
            level.add(new CategoryTreeDto(category.getId(), category.getName(), category.getSlug(),
                    category.getDescription(), category.getImageUrl(), category.getDisplayOrder(), children));
        }
        return level;
    }

    private void collectVisible(List<Category> categories, Map<Long, List<Category>> childrenByParent,
                                Long targetId, boolean insideTarget, Set<Long> ids, int depth) {
        for (Category category : categories) {
            boolean inside = insideTarget || category.getId().equals(targetId);
            if (inside) {
                ids.add(category.getId());
            }
            if (depth < MAX_DEPTH) {
                collectVisible(childrenByParent.getOrDefault(category.getId(), List.of()), childrenByParent,
                        targetId, inside, ids, depth + 1);
            }
        }
    }
}
