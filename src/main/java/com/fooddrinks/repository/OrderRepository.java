package com.fooddrinks.repository;

import com.fooddrinks.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Used by scheduled report job — fetch all orders within a date range
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
