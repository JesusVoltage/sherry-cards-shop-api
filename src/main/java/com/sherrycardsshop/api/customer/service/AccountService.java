package com.sherrycardsshop.api.customer.service;

import com.sherrycardsshop.api.auth.dto.UserDto;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.mapper.UserMapper;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.AuthSession;
import com.sherrycardsshop.api.auth.service.ClientInfo;
import com.sherrycardsshop.api.auth.service.LoginAttemptService;
import com.sherrycardsshop.api.auth.service.PasswordPolicy;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.customer.dto.PasswordChangeRequest;
import com.sherrycardsshop.api.customer.dto.ProfileRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de la cuenta del usuario autenticado. El correo no se modifica aquí: cambiarlo exige
 * verificar la nueva dirección, y eso llegará cuando la tienda envíe emails.
 */
@Service
public class AccountService {

    private static final String PASSWORD_ATTEMPTS_PREFIX = "password-change:";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final LoginAttemptService attemptService;
    private final UserMapper userMapper;

    public AccountService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          PasswordPolicy passwordPolicy, TokenService tokenService,
                          LoginAttemptService attemptService, UserMapper userMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.attemptService = attemptService;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserDto updateProfile(Long usuarioId, ProfileRequest request) {
        Usuario usuario = findActive(usuarioId);
        String username = request.username();
        if (!usuario.getUsername().equalsIgnoreCase(username) && usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe", "username");
        }
        usuario.setUsername(username);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellidos(request.apellidos() == null || request.apellidos().isBlank() ? null : request.apellidos().trim());
        try {
            usuarioRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe", "username");
        }
        return userMapper.toDto(usuario);
    }

    /**
     * Cambia (o crea, en cuentas solo de Google) la contraseña. Revoca todas las sesiones y
     * devuelve una nueva para el dispositivo actual, de modo que los demás deben volver a entrar.
     */
    @Transactional
    public AuthSession changePassword(Long usuarioId, PasswordChangeRequest request, ClientInfo client) {
        Usuario usuario = findActive(usuarioId);
        String attemptsKey = PASSWORD_ATTEMPTS_PREFIX + usuarioId;
        if (attemptService.isBlocked(attemptsKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Inténtalo más tarde");
        }

        String currentHash = usuario.getPasswordHash();
        if (currentHash != null) {
            String current = request.currentPassword();
            if (current == null || current.isEmpty() || !PasswordPolicy.fitsBcrypt(current)
                    || !passwordEncoder.matches(current, currentHash)) {
                attemptService.recordFailure(attemptsKey);
                // 400 y no 401: el usuario sigue autenticado, solo se ha equivocado de contraseña.
                throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta", "currentPassword");
            }
            attemptService.reset(attemptsKey);
            if (PasswordPolicy.fitsBcrypt(request.newPassword()) && passwordEncoder.matches(request.newPassword(), currentHash)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual", "newPassword");
            }
        }
        passwordPolicy.validate(request.newPassword(), usuario.getEmail(), usuario.getUsername(), "newPassword");

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        tokenService.revokeAllRefreshTokens(usuario);
        return new AuthSession(userMapper.toDto(usuario), tokenService.createAccessToken(usuario),
                tokenService.createRefreshToken(usuario, client));
    }

    private Usuario findActive(Long usuarioId) {
        return usuarioRepository.findDetailedById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión no válida o caducada"));
    }
}
