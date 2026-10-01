package com.sherrycardsshop.api.catalog.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminProductSummaryDto(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String categoryName,
        String type,
        String status,
        long variantCount,
        BigDecimal minPrice,
        long totalStock,
        String imageUrl,
        LocalDateTime updatedAt) {
}
