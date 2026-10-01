package com.sherrycardsshop.api.auth.repository;

import java.util.Optional;

import com.sherrycardsshop.api.auth.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByCode(String code);
}
