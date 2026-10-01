package com.sherrycardsshop.api.catalog.controller;

import com.sherrycardsshop.api.catalog.dto.admin.AdminProductDto;
import com.sherrycardsshop.api.catalog.dto.admin.AdminProductSummaryDto;
import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto;
import com.sherrycardsshop.api.catalog.dto.admin.ProductRequest;
import com.sherrycardsshop.api.catalog.service.AdminProductService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.common.dto.PageDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración · Productos", description = "Alta, edición y listado de productos desde el panel de control")
public class AdminProductController {

    private final AdminProductService productService;

    public AdminProductController(AdminProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    @Operation(summary = "Listar productos", description = "Busca por nombre, slug o SKU y filtra por categoría y estado.")
    public ApiResponse<PageDto<AdminProductSummaryDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Productos obtenidos correctamente", productService.list(search, categoryId, status, page, size));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Detalle de producto", description = "Producto con sus variantes e imágenes.")
    public ApiResponse<AdminProductDto> get(@PathVariable Long id) {
        return ApiResponse.success("Producto obtenido correctamente", productService.get(id));
    }

    @PostMapping("/products")
    @Operation(summary = "Crear producto", description = "Sin slug ni SKU se generan a partir del nombre.")
    public ResponseEntity<ApiResponse<AdminProductDto>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Producto creado correctamente", productService.create(request)));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Actualizar producto", description = "Sustituye datos, variantes e imágenes. Las variantes que no se envían se eliminan.")
    public ApiResponse<AdminProductDto> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Producto guardado correctamente", productService.update(id, request));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Borrar producto", description = "Solo si no tiene pedidos; si los tiene hay que archivarlo.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("Producto borrado correctamente", null);
    }

    @GetMapping("/catalog/options")
    @Operation(summary = "Opciones del formulario", description = "Categorías con su ruta completa, tipos y estados de producto.")
    public ApiResponse<CatalogOptionsDto> options() {
        return ApiResponse.success("Opciones obtenidas correctamente", productService.options());
    }
}
