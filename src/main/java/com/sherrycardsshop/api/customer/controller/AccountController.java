package com.sherrycardsshop.api.customer.controller;

import com.sherrycardsshop.api.auth.dto.UserDto;
import com.sherrycardsshop.api.auth.service.AuthSession;
import com.sherrycardsshop.api.auth.service.ClientInfo;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.customer.dto.PasswordChangeRequest;
import com.sherrycardsshop.api.customer.dto.ProfileRequest;
import com.sherrycardsshop.api.customer.service.AccountService;
import com.sherrycardsshop.api.security.AuthCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Mi cuenta", description = "Datos personales y contraseña del usuario autenticado")
public class AccountController {

    private final AccountService accountService;
    private final AuthCookies authCookies;

    public AccountController(AccountService accountService, AuthCookies authCookies) {
        this.accountService = accountService;
        this.authCookies = authCookies;
    }

    @PutMapping("/profile")
    @Operation(summary = "Actualizar datos personales", description = "Username, nombre y apellidos. El correo no se puede cambiar.")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Datos actualizados correctamente",
                accountService.updateProfile(Long.valueOf(jwt.getSubject()), request)));
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar contraseña",
            description = "Exige la contraseña actual (salvo cuentas solo de Google), cierra las demás sesiones y renueva la actual.")
    public ResponseEntity<ApiResponse<UserDto>> changePassword(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody PasswordChangeRequest request,
                                                               HttpServletRequest httpRequest) {
        ClientInfo client = new ClientInfo(httpRequest.getHeader(HttpHeaders.USER_AGENT), httpRequest.getRemoteAddr());
        AuthSession session = accountService.changePassword(Long.valueOf(jwt.getSubject()), request, client);
        return ResponseEntity.ok()
                .headers(authCookies.sessionHeaders(session.accessToken(), session.refreshToken()))
                .body(ApiResponse.success("Contraseña actualizada correctamente", session.user()));
    }
}
