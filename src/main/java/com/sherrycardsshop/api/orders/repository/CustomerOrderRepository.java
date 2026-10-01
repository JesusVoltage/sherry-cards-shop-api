package com.sherrycardsshop.api.orders.repository;

import java.util.Optional;

import com.sherrycardsshop.api.orders.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
}
