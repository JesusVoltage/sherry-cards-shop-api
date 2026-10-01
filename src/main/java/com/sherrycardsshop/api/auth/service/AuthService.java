package com.sherrycardsshop.api.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

import com.sherrycardsshop.api.auth.dto.LoginRequest;
import com.sherrycardsshop.api.auth.dto.RegisterRequest;
import com.sherrycardsshop.api.auth.dto.UserDto;
import com.sherrycardsshop.api.auth.entity.EstadoUsuario;
import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.mapper.UserMapper;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.GoogleTokenVerifier.GoogleUser;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.config.site.SiteProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MAX_USERNAME_BASE_LENGTH = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final LoginAttemptService loginAttemptService;
    private final UserMapper userMapper;
    private final SiteProperties site;
    // Se compara contra este hash cuando el email no existe para no revelar cuentas por tiempo de respuesta.
    private final String dummyPasswordHash;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       EstadoUsuarioRepository estadoUsuarioRepository, PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy, TokenService tokenService, GoogleTokenVerifier googleTokenVerifier,
                       LoginAttemptService loginAttemptService, UserMapper userMapper, SiteProperties site) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.estadoUsuarioRepository = estadoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.loginAttemptService = loginAttemptService;
        this.userMapper = userMapper;
        this.site = site;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (site.closed()) {
            throw siteClosed();
        }
        String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe", "username");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El email ya está registrado", "email");
        }
        passwordPolicy.validate(request.password(), email, request.username(), "password");

        Usuario usuario = newUsuario(email, request.username(), request.nombre().trim(), blankToNull(request.apellidos()));
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        return userMapper.toDto(saveNew(usuario));
    }

    @Transactional
    public AuthSession login(LoginRequest request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        if (loginAttemptService.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Inténtalo más tarde");
        }

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (!passwordMatches(request.password(), usuario == null ? null : usuario.getPasswordHash())) {
            loginAttemptService.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        loginAttemptService.reset(email);
        requireActive(usuario);
        return startSession(usuario, client);
    }

    @Transactional
    public AuthSession loginWithGoogle(String credential, ClientInfo client) {
        GoogleUser googleUser = googleTokenVerifier.verify(credential);
        if (!googleUser.emailVerified() || googleUser.email() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "La cuenta de Google no tiene un correo verificado");
        }

        Usuario usuario = usuarioRepository.findByGoogleSub(googleUser.subject())
                .orElseGet(() -> linkOrCreateGoogleUser(googleUser));
        requireActive(usuario);
        return startSession(usuario, client);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession refresh(String refreshToken, ClientInfo client) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw TokenService.invalidSession();
        }
        Usuario usuario = tokenService.consumeRefreshToken(refreshToken);
        if (!usuario.isActivo()) {
            throw TokenService.invalidSession();
        }
        requireSiteAccess(usuario);
        return new AuthSession(userMapper.toDto(usuario), tokenService.createAccessToken(usuario),
                tokenService.createRefreshToken(usuario, client));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long usuarioId) {
        return usuarioRepository.findDetailedById(usuarioId)
                .filter(Usuario::isActivo)
                .map(userMapper::toDto)
                .orElseThrow(TokenService::invalidSession);
    }

    private Usuario linkOrCreateGoogleUser(GoogleUser googleUser) {
        String email = normalizeEmail(googleUser.email());
        Usuario existing = usuarioRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getEmailVerificadoAt() == null) {
                // Google acaba de demostrar quién controla este correo. Si la cuenta local nunca se
                // verificó, pudo crearla otra persona: se anulan su contraseña y sus sesiones.
                existing.setPasswordHash(null);
                existing.setEmailVerificadoAt(LocalDateTime.now());
                tokenService.revokeAllRefreshTokens(existing);
            }
            existing.setGoogleSub(googleUser.subject());
            return existing;
        }

        String nombre = firstNonBlank(googleUser.givenName(), googleUser.name(), email.substring(0, email.indexOf('@')));
        Usuario usuario = newUsuario(email, generateUsername(email), truncate(nombre, 100),
                truncate(blankToNull(googleUser.familyName()), 150));
        usuario.setGoogleSub(googleUser.subject());
        usuario.setEmailVerificadoAt(LocalDateTime.now());
        return saveNew(usuario);
    }

    private Usuario newUsuario(String email, String username, String nombre, String apellidos) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setRol(rolRepository.findByCode(Rol.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + Rol.CLIENTE)));
        usuario.setEstado(estadoUsuarioRepository.findByCode(EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Falta el estado " + EstadoUsuario.ACTIVO)));
        return usuario;
    }

    private Usuario saveNew(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            // Otra petición registró el mismo username o email entre la comprobación y el insert.
            throw new ApiException(HttpStatus.CONFLICT, "La cuenta ya existe");
        }
    }

    private AuthSession startSession(Usuario usuario, ClientInfo client) {
        requireSiteAccess(usuario);
        usuario.setUltimoAccesoAt(LocalDateTime.now());
        return new AuthSession(userMapper.toDto(usuario), tokenService.createAccessToken(usuario),
                tokenService.createRefreshToken(usuario, client));
    }

    private boolean passwordMatches(String password, String passwordHash) {
        if (!PasswordPolicy.fitsBcrypt(password)) {
            return false;
        }
        boolean matches = passwordEncoder.matches(password, passwordHash == null ? dummyPasswordHash : passwordHash);
        return passwordHash != null && matches;
    }

    private void requireActive(Usuario usuario) {
        switch (usuario.getEstado().getCode()) {
            case EstadoUsuario.ACTIVO -> { }
            case EstadoUsuario.BLOQUEADO -> throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta está bloqueada");
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "La cuenta está pendiente de activación");
        }
    }

    private void requireSiteAccess(Usuario usuario) {
        if (site.closed() && !Rol.ADMIN.equals(usuario.getRol().getCode())) {
            throw siteClosed();
        }
    }

    private static ApiException siteClosed() {
        return new ApiException(HttpStatus.FORBIDDEN, "La tienda todavía no está abierta");
    }

    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9_-]", "");
        if (base.length() < 3) {
            base = "usuario";
        }
        base = truncate(base, MAX_USERNAME_BASE_LENGTH);
        String candidate = base;
        for (int attempt = 0; usuarioRepository.existsByUsernameIgnoreCase(candidate); attempt++) {
            if (attempt == 20) {
                throw new IllegalStateException("No se pudo generar un username libre");
            }
            candidate = base + "-" + (1000 + RANDOM.nextInt(9000));
        }
        return candidate;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "Usuario";
    }
}
