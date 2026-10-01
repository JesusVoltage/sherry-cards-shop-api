package com.sherrycardsshop.api.catalog.dto.admin;

import java.util.List;

/** Valores para los desplegables del formulario de producto. */
public record CatalogOptionsDto(List<CategoryOption> categories, List<CodeName> types, List<CodeName> statuses) {

    /** {@code path} muestra la rama completa: "Pokémon › Expansiones › Escarlata y Púrpura". */
    public record CategoryOption(Long id, String name, String path, boolean active) {
    }

    public record CodeName(String code, String name) {
    }
}
