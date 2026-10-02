package com.sherrycardsshop.api.admin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.sherrycardsshop.api.admin.dto.AdminUserDto;
import com.sherrycardsshop.api.admin.dto.AdminUserRequest;
import com.sherrycardsshop.api.admin.dto.UserOptionsDto;
import com.sherrycardsshop.api.auth.entity.EstadoUsuario;
import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.TokenAutenticacionRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.auth.service.PasswordPolicy;
import com.sherrycardsshop.api.auth.service.TokenService;
import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto.CodeName;
import com.sherrycardsshop.api.common.dto.PageDto;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.customer.repository.DireccionUsuarioRepository;
import com.sherrycardsshop.api.orders.repository.CustomerOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas desde el panel. Nunca deja la tienda sin un administrador activo: nadie puede quitarse
 * el rol ni bloquearse a sí mismo, ni dejar fuera al último administrador.
 */
@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoRepository;
    private final TokenAutenticacionRepository tokenRepository;
    private final DireccionUsuarioRepository direccionRepository;
    private final CustomerOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;

    public AdminUserService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                            EstadoUsuarioRepository estadoRepository, TokenAutenticacionRepository tokenRepository,
                            DireccionUsuarioRepository direccionRepository, CustomerOrderRepository orderRepository,
                            PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.estadoRepository = estadoRepository;
        this.tokenRepository = tokenRepository;
        this.direccionRepository = direccionRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public PageDto<AdminUserDto> list(String search, String role, String status, int page, int size) {
        String pattern = search == null || search.isBlank() ? null : "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageDto.of(usuarioRepository.searchForAdmin(pattern, code(role), code(status), pageable),
                users -> users.stream().map(AdminUserService::toDto).toList());
    }

    @Transactional(readOnly = true)
    public AdminUserDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public AdminUserDto create(AdminUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria", "password");
        }
        Usuario usuario = new Usuario();
        apply(usuario, request);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        return save(usuario);
    }

    @Transactional
    public AdminUserDto update(Long id, AdminUserRequest request, Long actorId) {
        Usuario usuario = find(id);
        String previousEmail = usuario.getEmail();
        boolean wasActiveAdmin = activeAdmin(usuario);
        String previousRole = usuario.getRol().getCode();
        String previousStatus = usuario.getEstado().getCode();

        apply(usuario, request);
        boolean activeAdmin = activeAdmin(usuario);
        if (id.equals(actorId) && !activeAdmin) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No puedes quitarte el rol de administrador ni bloquear tu propia cuenta",
                    Rol.ADMIN.equals(usuario.getRol().getCode()) ? "status" : "role");
        }
        if (wasActiveAdmin && !activeAdmin
                && usuarioRepository.countByRolCodeAndEstadoCode(Rol.ADMIN, EstadoUsuario.ACTIVO) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Tiene que quedar al menos un administrador activo");
        }
        boolean passwordChanged = request.password() != null && !request.password().isBlank();
        if (passwordChanged) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        boolean emailChanged = !previousEmail.equals(usuario.getEmail());
        if (emailChanged) {
            usuario.setEmailVerificadoAt(null);
        }
        // Rol, estado, correo o contraseña nuevos invalidan las sesiones abiertas: el cambio se aplica ya
        // y no cuando caduque el refresh token (el access token dura como mucho 15 minutos).
        if (passwordChanged || emailChanged || !previousRole.equals(usuario.getRol().getCode())
                || !previousStatus.equals(usuario.getEstado().getCode())) {
            tokenService.revokeAllRefreshTokens(usuario);
        }
        return save(usuario);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Usuario usuario = find(id);
        if (id.equals(actorId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes borrar tu propia cuenta");
        }
        if (orderRepository.existsByUserId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tiene pedidos. Bloquéala en lugar de borrarla");
        }
        if (activeAdmin(usuario) && usuarioRepository.countByRolCodeAndEstadoCode(Rol.ADMIN, EstadoUsuario.ACTIVO) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Tiene que quedar al menos un administrador activo");
        }
        tokenRepository.deleteAllByUsuarioId(id);
        direccionRepository.deleteAllByUsuarioId(id);
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public UserOptionsDto options() {
        List<CodeName> roles = rolRepository.findAll().stream().sorted(Comparator.comparing(Rol::getId))
                .map(rol -> new CodeName(rol.getCode(), rol.getName())).toList();
        List<CodeName> statuses = estadoRepository.findAll().stream().sorted(Comparator.comparing(EstadoUsuario::getId))
                .map(estado -> new CodeName(estado.getCode(), estado.getName())).toList();
        return new UserOptionsDto(roles, statuses);
    }

    private void apply(Usuario usuario, AdminUserRequest request) {
        Long id = usuario.getId() == null ? -1L : usuario.getId();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        String username = request.username().strip();
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe", "username");
        }
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "El email ya está registrado", "email");
        }
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicy.validate(request.password(), email, username, "password");
        }
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombre(request.nombre().strip());
        usuario.setApellidos(request.apellidos() == null || request.apellidos().isBlank() ? null : request.apellidos().strip());
        usuario.setRol(rolRepository.findByCode(code(request.role())).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Rol no válido", "role")));
        usuario.setEstado(estadoRepository.findByCode(code(request.status())).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Estado no válido", "status")));
    }

    private AdminUserDto save(Usuario usuario) {
        try {
            return toDto(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El username o el email ya están en uso");
        }
    }

    private Usuario find(Long id) {
        return usuarioRepository.findDetailedById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private static boolean activeAdmin(Usuario usuario) {
        return Rol.ADMIN.equals(usuario.getRol().getCode()) && EstadoUsuario.ACTIVO.equals(usuario.getEstado().getCode());
    }

    private static String code(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    private static AdminUserDto toDto(Usuario usuario) {
        return new AdminUserDto(usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getNombre(),
                usuario.getApellidos(), usuario.getRol().getCode(), usuario.getEstado().getCode(),
                usuario.getEmailVerificadoAt(), usuario.getUltimoAccesoAt(), usuario.getCreatedAt(),
                usuario.getPasswordHash() != null, Objects.nonNull(usuario.getGoogleSub()));
    }
}
