package com.sherrycardsshop.api.auth.repository;

import java.util.Optional;

import com.sherrycardsshop.api.auth.entity.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoUsuarioRepository extends JpaRepository<EstadoUsuario, Long> {

    Optional<EstadoUsuario> findByCode(String code);
}
