package com.sherrycardsshop.api.catalog.dto.admin;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Nuevo orden de las hijas de {@code parentId} (o de las raíces si es nulo). */
public record CategoryReorderRequest(Long parentId, @NotEmpty(message = "Faltan las categorías") List<Long> categoryIds) {
}
