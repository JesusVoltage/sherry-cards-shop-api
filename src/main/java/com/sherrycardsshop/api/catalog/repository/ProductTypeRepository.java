package com.sherrycardsshop.api.catalog.repository;

import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    Optional<ProductType> findByCode(String code);
}
