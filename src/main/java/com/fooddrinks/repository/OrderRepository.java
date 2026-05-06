package com.fooddrinks.repository;

import com.fooddrinks.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Lightweight history list — does NOT load items (use findByIdWithItems for detail).
     * Items.size() will still work for itemCount via lazy load (within @Transactional).
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Full detail for a single order with items JOIN FETCHed.
     * Prevents LazyInitializationException when mapping items outside TX.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    // Used by scheduled report job — fetch all orders within a date range
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
