package com.sherrycardsshop.api.orders.repository;

import java.util.Optional;

import com.sherrycardsshop.api.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {

    Optional<OrderStatus> findByCode(String code);
}
