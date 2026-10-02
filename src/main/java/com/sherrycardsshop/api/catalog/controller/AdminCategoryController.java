package com.sherrycardsshop.api.catalog.controller;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.admin.AdminCategoryDto;
import com.sherrycardsshop.api.catalog.dto.admin.CategoryReorderRequest;
import com.sherrycardsshop.api.catalog.dto.admin.CategoryRequest;
import com.sherrycardsshop.api.catalog.service.AdminCategoryService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "Administración · Categorías", description = "Árbol de categorías y subcategorías desde el panel de control")
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    public AdminCategoryController(AdminCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Todas, activas u ocultas, con su padre y número de productos.")
    public ApiResponse<List<AdminCategoryDto>> list() {
        return ApiResponse.success("Categorías obtenidas correctamente", categoryService.list());
    }

    @PostMapping
    @Operation(summary = "Crear categoría", description = "Sin padre es raíz; se coloca la última de su nivel.")
    public ResponseEntity<ApiResponse<AdminCategoryDto>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Categoría creada correctamente", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría", description = "Permite moverla de padre, siempre sin ciclos y sin pasar de 6 niveles.")
    public ApiResponse<AdminCategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Categoría guardada correctamente", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar categoría", description = "Solo sin subcategorías; sus productos pasan a Sin categoría.")
    public ApiResponse<Integer> delete(@PathVariable Long id) {
        int moved = categoryService.delete(id);
        String message = moved == 0 ? "Categoría borrada correctamente"
                : "Categoría borrada. " + moved + (moved == 1 ? " producto pasa" : " productos pasan") + " a Sin categoría";
        return ApiResponse.success(message, moved);
    }

    @PutMapping("/order")
    @Operation(summary = "Reordenar un nivel", description = "Recibe todas las categorías de un mismo padre en el orden deseado.")
    public ApiResponse<List<AdminCategoryDto>> reorder(@Valid @RequestBody CategoryReorderRequest request) {
        return ApiResponse.success("Orden guardado correctamente", categoryService.reorder(request));
    }
}
