package com.sherrycardsshop.api.customer.repository;

import java.util.List;
import java.util.Optional;

import com.sherrycardsshop.api.customer.entity.DireccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DireccionUsuarioRepository extends JpaRepository<DireccionUsuario, Long> {

    List<DireccionUsuario> findAllByUsuarioIdOrderByIdAsc(Long usuarioId);

    Optional<DireccionUsuario> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndPredeterminadaEnvioTrue(Long usuarioId);

    boolean existsByUsuarioIdAndPredeterminadaFacturacionTrue(Long usuarioId);

    Optional<DireccionUsuario> findFirstByUsuarioIdAndUsoEnvioTrueAndIdNotOrderByIdAsc(Long usuarioId, Long exceptId);

    Optional<DireccionUsuario> findFirstByUsuarioIdAndUsoFacturacionTrueAndIdNotOrderByIdAsc(Long usuarioId, Long exceptId);

    @Modifying(flushAutomatically = true)
    @Query("update DireccionUsuario d set d.predeterminadaEnvio = false "
            + "where d.usuario.id = :usuarioId and d.id <> :exceptId and d.predeterminadaEnvio = true")
    int clearDefaultEnvio(@Param("usuarioId") Long usuarioId, @Param("exceptId") Long exceptId);

    @Modifying(flushAutomatically = true)
    @Query("update DireccionUsuario d set d.predeterminadaFacturacion = false "
            + "where d.usuario.id = :usuarioId and d.id <> :exceptId and d.predeterminadaFacturacion = true")
    int clearDefaultFacturacion(@Param("usuarioId") Long usuarioId, @Param("exceptId") Long exceptId);
}
