package com.sherrycardsshop.api.admin.controller;

import com.sherrycardsshop.api.admin.dto.AdminUserDto;
import com.sherrycardsshop.api.admin.dto.AdminUserRequest;
import com.sherrycardsshop.api.admin.dto.UserOptionsDto;
import com.sherrycardsshop.api.admin.service.AdminUserService;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.common.dto.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping("/api/admin/users")
@Tag(name = "Administración · Usuarios", description = "Alta, edición, roles y bloqueo de cuentas desde el panel de control")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Busca por username, email o nombre y filtra por rol y estado.")
    public ApiResponse<PageDto<AdminUserDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Usuarios obtenidos correctamente", userService.list(search, role, status, page, size));
    }

    @GetMapping("/options")
    @Operation(summary = "Roles y estados", description = "Valores para los desplegables del formulario de usuario.")
    public ApiResponse<UserOptionsDto> options() {
        return ApiResponse.success("Opciones obtenidas correctamente", userService.options());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de usuario")
    public ApiResponse<AdminUserDto> get(@PathVariable Long id) {
        return ApiResponse.success("Usuario obtenido correctamente", userService.get(id));
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Con cualquier rol y estado. La contraseña sigue la política de la tienda.")
    public ResponseEntity<ApiResponse<AdminUserDto>> create(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario creado correctamente", userService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Sin contraseña la conserva. Cambiar rol, estado, correo o contraseña cierra sus sesiones.")
    public ApiResponse<AdminUserDto> update(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Usuario guardado correctamente", userService.update(id, request, Long.valueOf(jwt.getSubject())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar usuario", description = "Solo cuentas sin pedidos y nunca la propia.")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        userService.delete(id, Long.valueOf(jwt.getSubject()));
        return ApiResponse.success("Usuario borrado correctamente", null);
    }
}
