package com.sherrycardsshop.api.catalog.controller;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.dto.CategoryTreeDto;
import com.sherrycardsshop.api.catalog.service.CategoryService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorías", description = "Consulta de categorías y subcategorías activas")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías raíz activas", description = "Devuelve las categorías principales activas ordenadas por display_order ascendente.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categorías activas")
    public ApiResponse<List<CategoryDto>> getActiveCategories() {
        return ApiResponse.success("Categorías activas obtenidas correctamente", categoryService.getActiveCategories());
    }

    @GetMapping("/tree")
    @Operation(summary = "Árbol de categorías", description = "Categorías activas con sus subcategorías anidadas, para menús de navegación.")
    public ApiResponse<List<CategoryTreeDto>> getCategoryTree() {
        return ApiResponse.success("Árbol de categorías obtenido correctamente", categoryService.getCategoryTree());
    }
}