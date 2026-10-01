package com.sherrycardsshop.api.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code currentPassword} es obligatoria salvo para cuentas que solo usan Google y aún no
 * tienen contraseña.
 */
public record PasswordChangeRequest(
        @Size(max = 128)
        String currentPassword,

        @NotBlank @Size(max = 128)
        String newPassword) {
}
