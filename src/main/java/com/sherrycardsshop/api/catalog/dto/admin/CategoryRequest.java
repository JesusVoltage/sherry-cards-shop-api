package com.sherrycardsshop.api.catalog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sin slug se genera a partir del nombre. Sin padre es una categoría raíz. */
public record CategoryRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres") String name,
        @Size(max = 100, message = "Máximo 100 caracteres") String slug,
        @Size(max = 500, message = "Máximo 500 caracteres") String description,
        @Size(max = 500, message = "Máximo 500 caracteres")
        @Pattern(regexp = "^(https://\\S+)?$", message = "La URL debe empezar por https://") String imageUrl,
        Long parentId,
        boolean active) {
}
