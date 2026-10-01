package com.sherrycardsshop.api.customer.controller;

import java.util.List;

import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.customer.dto.AddressDto;
import com.sherrycardsshop.api.customer.dto.AddressRequest;
import com.sherrycardsshop.api.customer.service.AddressService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/addresses")
@Tag(name = "Direcciones", description = "Direcciones de envío y facturación del usuario autenticado")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "Listar mis direcciones")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Direcciones obtenidas correctamente",
                addressService.getAddresses(userId(jwt))));
    }

    @PostMapping
    @Operation(summary = "Añadir dirección", description = "La primera dirección de envío o de facturación pasa a ser la predeterminada.")
    public ResponseEntity<ApiResponse<AddressDto>> createAddress(@AuthenticationPrincipal Jwt jwt,
                                                                 @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Dirección añadida correctamente",
                addressService.createAddress(userId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar dirección")
    public ResponseEntity<ApiResponse<AddressDto>> updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                                 @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dirección actualizada correctamente",
                addressService.updateAddress(userId(jwt), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dirección")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        addressService.deleteAddress(userId(jwt), id);
        return ResponseEntity.ok(ApiResponse.success("Dirección eliminada correctamente", null));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
