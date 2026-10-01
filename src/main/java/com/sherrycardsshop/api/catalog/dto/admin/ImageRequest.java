package com.sherrycardsshop.api.catalog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ImageRequest(
        @NotBlank(message = "Falta la URL de la imagen") @Size(max = 500, message = "Máximo 500 caracteres")
        @Pattern(regexp = "^https://\\S+$", message = "La URL debe empezar por https://") String url,
        @Size(max = 200, message = "Máximo 200 caracteres") String altText) {
}
