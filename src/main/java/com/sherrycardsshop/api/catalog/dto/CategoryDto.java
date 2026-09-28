package com.sherrycardsshop.api.catalog.dto;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        int displayOrder) {
}