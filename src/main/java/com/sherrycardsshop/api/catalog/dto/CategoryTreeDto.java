package com.sherrycardsshop.api.catalog.dto;

import java.util.List;

/** Categoría con sus subcategorías activas, para menús y navegación. */
public record CategoryTreeDto(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        int displayOrder,
        List<CategoryTreeDto> children) {
}
