package com.sherrycardsshop.api.auth.controller;

import com.sherrycardsshop.api.auth.dto.GoogleLoginRequest;
import com.sherrycardsshop.api.auth.dto.LoginRequest;
import com.sherrycardsshop.api.auth.dto.RegisterRequest;
import com.sherrycardsshop.api.auth.dto.UserDto;
import com.sherrycardsshop.api.auth.service.AuthService;
import com.sherrycardsshop.api.auth.service.AuthSession;
import com.sherrycardsshop.api.auth.service.ClientInfo;
import com.sherrycardsshop.api.common.dto.ApiResponse;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.security.AuthCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, login con email o Google y sesión mediante cookies HttpOnly")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea una cuenta de cliente. No inicia sesión.")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cuenta creada correctamente", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con email y contraseña", description = "Establece las cookies de sesión.")
    public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest) {
        return sessionResponse("Sesión iniciada correctamente", authService.login(request, clientInfo(httpRequest)));
    }

    @PostMapping("/google")
    @Operation(summary = "Iniciar sesión con Google", description = "Recibe el ID token de Google Identity Services, crea o vincula la cuenta y establece las cookies de sesión.")
    public ResponseEntity<ApiResponse<UserDto>> google(@Valid @RequestBody GoogleLoginRequest request,
                                                       HttpServletRequest httpRequest) {
        return sessionResponse("Sesión iniciada correctamente",
                authService.loginWithGoogle(request.credential(), clientInfo(httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar sesión", description = "Rota el refresh token de la cookie y emite un nuevo access token.")
    public ResponseEntity<ApiResponse<UserDto>> refresh(
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        try {
            return sessionResponse("Sesión renovada correctamente", authService.refresh(refreshToken, clientInfo(httpRequest)));
        } catch (ApiException exception) {
            return clearedSessionResponse(exception.getStatus(), ApiResponse.failure(exception.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token y borra las cookies. Es idempotente.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return clearedSessionResponse(HttpStatus.OK, ApiResponse.success("Sesión cerrada correctamente", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Devuelve el perfil del usuario autenticado.")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal Jwt jwt) {
        UserDto user = authService.getCurrentUser(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ApiResponse.success("Usuario obtenido correctamente", user));
    }

    private ResponseEntity<ApiResponse<UserDto>> sessionResponse(String message, AuthSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.accessToken(session.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookies.refreshToken(session.refreshToken()).toString())
                .body(ApiResponse.success(message, session.user()));
    }

    private <T> ResponseEntity<ApiResponse<T>> clearedSessionResponse(HttpStatus status, ApiResponse<T> body) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, authCookies.clearAccessToken().toString())
                .header(HttpHeaders.SET_COOKIE, authCookies.clearRefreshToken().toString())
                .body(body);
    }

    private static ClientInfo clientInfo(HttpServletRequest request) {
        return new ClientInfo(request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
    }
}
