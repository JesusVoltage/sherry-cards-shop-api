package com.sherrycardsshop.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Usa letras A-Z, números, guion o guion bajo")
        String username,

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @Size(max = 100)
        String nombre,

        @Size(max = 150)
        String apellidos,

        @NotBlank @Size(min = 8, max = 72)
        String password) {
}
