package com.sherrycardsshop.api.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta o edición de una cuenta desde el panel. La contraseña es obligatoria solo al crear. */
public record AdminUserRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio") @Size(min = 3, max = 30, message = "Entre 3 y 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Usa letras A-Z, números, guion o guion bajo") String username,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 254, message = "Correo demasiado largo")
        String email,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres") String nombre,
        @Size(max = 150, message = "Máximo 150 caracteres") String apellidos,
        @Size(max = 128, message = "Máximo 128 caracteres") String password,
        @NotBlank(message = "Elige un rol") String role,
        @NotBlank(message = "Elige un estado") String status) {
}
