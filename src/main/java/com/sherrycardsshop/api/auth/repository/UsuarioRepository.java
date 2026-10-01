package com.sherrycardsshop.api.auth.repository;

import java.util.Optional;

import com.sherrycardsshop.api.auth.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findByGoogleSub(String googleSub);

    @EntityGraph(attributePaths = {"rol", "estado"})
    Optional<Usuario> findDetailedById(Long id);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
