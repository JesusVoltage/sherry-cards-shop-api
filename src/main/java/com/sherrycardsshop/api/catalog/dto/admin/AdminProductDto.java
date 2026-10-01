package com.sherrycardsshop.api.catalog.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminProductDto(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String type,
        String status,
        String description,
        LocalDate releaseDate,
        List<AdminVariantDto> variants,
        List<AdminImageDto> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
