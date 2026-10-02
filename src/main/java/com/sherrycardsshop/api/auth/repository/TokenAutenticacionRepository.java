package com.sherrycardsshop.api.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.sherrycardsshop.api.auth.entity.TokenAutenticacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenAutenticacionRepository extends JpaRepository<TokenAutenticacion, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "usuario.estado"})
    Optional<TokenAutenticacion> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenAutenticacion t set t.revokedAt = :now where t.usuario.id = :usuarioId and t.revokedAt is null")
    int revokeAllActiveByUsuarioId(@Param("usuarioId") Long usuarioId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from TokenAutenticacion t where t.usuario.id = :usuarioId")
    int deleteAllByUsuarioId(@Param("usuarioId") Long usuarioId);
}
