package com.sherrycardsshop.api.catalog.dto.admin;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta o edición completa de un producto. Sin slug ni SKU se generan a partir del nombre. */
public record ProductRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 200, message = "Máximo 200 caracteres") String name,
        @Size(max = 200, message = "Máximo 200 caracteres") String slug,
        @NotNull(message = "Elige una categoría") Long categoryId,
        @NotBlank(message = "Elige un tipo de producto") String type,
        @NotBlank(message = "Elige un estado") String status,
        @Size(max = 4000, message = "Máximo 4000 caracteres") String description,
        LocalDate releaseDate,
        @NotEmpty(message = "Añade al menos una variante") @Size(max = 30, message = "Máximo 30 variantes")
        List<@Valid @NotNull VariantRequest> variants,
        @Size(max = 12, message = "Máximo 12 imágenes") List<@Valid @NotNull ImageRequest> images) {
}
