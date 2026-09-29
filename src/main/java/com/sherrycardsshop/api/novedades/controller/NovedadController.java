package com.sherrycardsshop.api.novedades.controller;

import java.util.List;

import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.novedades.dto.NovedadDto;
import com.sherrycardsshop.api.novedades.service.NovedadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/novedades")
@Tag(name = "Novedades", description = "Consulta de novedades activas")
public class NovedadController {

    private final NovedadService novedadService;

    public NovedadController(NovedadService novedadService) {
        this.novedadService = novedadService;
    }

    @GetMapping
    @Operation(summary = "Listar novedades activas", description = "Devuelve novedades activas ordenadas por display_order ascendente.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Novedades activas")
    public ResponseEntity<ApiResponse<List<NovedadDto>>> getActiveNovedades() {
        return ResponseEntity.ok(ApiResponse.success("Novedades activas obtenidas correctamente", novedadService.getActiveNovedades()));
    }
}
