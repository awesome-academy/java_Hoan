package com.fooddrinks.repository;

import com.fooddrinks.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /**
     * Eagerly loads cart items and their associated products in a single JOIN
     * FETCH.
     * Use this when rendering the cart to avoid N+1 queries on items.product.
     * Product.images are still lazy but will batch-load via @BatchSize(20) on
     * Product.
     */
    @EntityGraph(attributePaths = { "items", "items.product" })
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findWithItemsByUserId(@Param("userId") Long userId);

    /**
     * Acquires a row-level PESSIMISTIC_WRITE lock on the cart row (SELECT ... FOR
     * UPDATE).
     * Use in placeOrder() to prevent two concurrent requests from the same user
     * (e.g. double-click "Place Order") from both reading the same cart and
     * creating duplicate orders.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithLock(@Param("userId") Long userId);
}
