package com.sherrycardsshop.api.catalog.repository;

import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);
}
