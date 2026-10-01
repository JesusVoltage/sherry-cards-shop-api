package com.sherrycardsshop.api.catalog.dto.admin;

import java.math.BigDecimal;

public record AdminVariantDto(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        BigDecimal compareAtPrice,
        BigDecimal vatRate,
        int stockQuantity,
        Integer weightGrams,
        boolean active) {
}
