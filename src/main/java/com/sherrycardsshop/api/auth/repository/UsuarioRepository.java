package com.sherrycardsshop.api.auth.repository;

import java.util.Optional;

import com.sherrycardsshop.api.auth.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findByGoogleSub(String googleSub);

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findDetailedById(Long id);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRolCode(String code);

    long countByRolCodeAndEstadoCode(String rolCode, String estadoCode);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    /** Búsqueda del panel: {@code search} ya llega en minúsculas y con comodines, o nulo. */
    @Query(value = """
            select u from Usuario u join fetch u.rol join fetch u.estado
            where (:role is null or u.rol.code = :role)
              and (:status is null or u.estado.code = :status)
              and (:search is null or lower(u.username) like :search or lower(u.email) like :search
                   or lower(u.nombre) like :search or lower(coalesce(u.apellidos, '')) like :search)
            order by u.createdAt desc, u.id desc
            """, countQuery = """
            select count(u) from Usuario u
            where (:role is null or u.rol.code = :role)
              and (:status is null or u.estado.code = :status)
              and (:search is null or lower(u.username) like :search or lower(u.email) like :search
                   or lower(u.nombre) like :search or lower(coalesce(u.apellidos, '')) like :search)
            """)
    Page<Usuario> searchForAdmin(@Param("search") String search, @Param("role") String role,
                                 @Param("status") String status, Pageable pageable);
}
