package com.sherrycardsshop.api.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Usa letras A-Z, números, guion o guion bajo")
        String username,

        @NotBlank @Size(max = 100)
        String nombre,

        @Size(max = 150)
        String apellidos) {
}
