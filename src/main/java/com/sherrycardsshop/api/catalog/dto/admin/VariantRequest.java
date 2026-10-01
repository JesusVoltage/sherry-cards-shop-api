package com.sherrycardsshop.api.catalog.dto.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Variante a crear (sin id) o a actualizar (con id). Las que no se envían se eliminan. */
public record VariantRequest(
        Long id,
        @Size(max = 64, message = "Máximo 64 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Solo letras, números, puntos, guiones y guiones bajos")
        String sku,
        @NotBlank(message = "El nombre de la variante es obligatorio") @Size(max = 150, message = "Máximo 150 caracteres")
        String name,
        @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Precio no válido") BigDecimal price,
        @DecimalMin(value = "0.00", message = "El precio anterior no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Precio no válido") BigDecimal compareAtPrice,
        @NotNull(message = "El IVA es obligatorio") @DecimalMin(value = "0.00", message = "IVA no válido")
        @DecimalMax(value = "100.00", message = "IVA no válido") BigDecimal vatRate,
        @NotNull(message = "El stock es obligatorio") @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 1_000_000, message = "Stock no válido") Integer stockQuantity,
        @Positive(message = "El peso debe ser mayor que 0") Integer weightGrams,
        boolean active) {
}
