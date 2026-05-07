package com.fooddrinks.dto.response;

import com.fooddrinks.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight summary used in the order history list.
 * Does NOT include order items — use OrderResponse for full detail.
 */
@Getter
@Builder
@AllArgsConstructor
public class OrderSummaryResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String note;
    private int itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor for JPQL constructor expression — COUNT(i) returns Long.
     * Converts Long → int for itemCount.
     */
    public OrderSummaryResponse(Long id, OrderStatus status, BigDecimal totalAmount,
            String shippingAddress, String note, Long itemCount,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.itemCount = itemCount != null ? itemCount.intValue() : 0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
