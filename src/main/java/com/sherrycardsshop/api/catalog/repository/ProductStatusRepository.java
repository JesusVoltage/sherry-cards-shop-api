package com.sherrycardsshop.api.catalog.repository;

import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStatusRepository extends JpaRepository<ProductStatus, Long> {

    Optional<ProductStatus> findByCode(String code);
}
