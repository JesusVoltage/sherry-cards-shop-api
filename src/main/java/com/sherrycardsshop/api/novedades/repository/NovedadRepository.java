package com.sherrycardsshop.api.novedades.repository;

import java.util.List;

import com.sherrycardsshop.api.novedades.entity.Novedad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    @EntityGraph(attributePaths = "category")
    List<Novedad> findAllByActiveTrueOrderByDisplayOrderAsc();
}
