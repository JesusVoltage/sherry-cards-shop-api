package com.sherrycardsshop.api.admin.controller;

import com.sherrycardsshop.api.admin.dto.DashboardDto;
import com.sherrycardsshop.api.admin.service.DashboardService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Administración · Resumen", description = "Cifras generales del panel de control")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Resumen del panel", description = "Productos por estado, categorías, usuarios y almacenamiento de imágenes usado.")
    public ApiResponse<DashboardDto> summary() {
        return ApiResponse.success("Resumen obtenido correctamente", dashboardService.summary());
    }
}
