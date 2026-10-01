package com.sherrycardsshop.api.cart.repository;

import java.util.Optional;

import com.sherrycardsshop.api.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
}
