package com.sherrycardsshop.api.catalog.dto.admin;

/** Categoría para el panel. El front monta el árbol a partir de {@code parentId}. */
public record AdminCategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Long parentId,
        boolean active,
        int displayOrder,
        long productCount,
        long childCount,
        boolean system) {
}
