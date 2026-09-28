package com.sherrycardsshop.api.catalog.controller;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.service.CategoryService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorías", description = "Consulta de categorías activas")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías activas", description = "Devuelve categorías activas ordenadas por display_order ascendente.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categorías activas")
    public ApiResponse<List<CategoryDto>> getActiveCategories() {
        return ApiResponse.success("Categorías activas obtenidas correctamente", categoryService.getActiveCategories());
    }
}