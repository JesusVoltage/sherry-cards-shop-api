package com.sherrycardsshop.api.novedades.dto;

public record NovedadDto(
        Long id,
        String title,
        String slug,
        String description,
        String imageUrl,
        String categoryName,
        String categorySlug,
        int displayOrder) {
}