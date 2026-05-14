package com.fooddrinks.repository;

import com.fooddrinks.dto.response.OrderSummaryResponse;
import com.fooddrinks.entity.Order;
import com.fooddrinks.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * History list with itemCount computed in the DB via COUNT — avoids loading the
     * items collection just to call size(). Groups by order PK (MySQL ONLY_FULL_GROUP_BY
     * allows all non-aggregated columns to be functionally dependent on the PK).
     */
    @Query("SELECT new com.fooddrinks.dto.response.OrderSummaryResponse("
            + "o.id, o.status, o.totalAmount, o.shippingAddress, o.note, "
            + "COUNT(i), o.createdAt, o.updatedAt) "
            + "FROM Order o LEFT JOIN o.items i "
            + "WHERE o.user.id = :userId "
            + "GROUP BY o.id, o.status, o.totalAmount, o.shippingAddress, o.note, o.createdAt, o.updatedAt "
            + "ORDER BY o.createdAt DESC")
    List<OrderSummaryResponse> findSummaryByUserId(@Param("userId") Long userId);

    /**
     * Full detail for a single order with items JOIN FETCHed.
     * Prevents LazyInitializationException when mapping items outside TX.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    // Used by scheduled report job — fetch all orders within a date range
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Admin: list all orders with user eager-loaded (avoids N+1 on admin order list).
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(Pageable pageable);

    long countByStatus(OrderStatus status);

    /** Total revenue = sum of totalAmount for all COMPLETED orders. COALESCE guards against NULL when no rows exist. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);
}
