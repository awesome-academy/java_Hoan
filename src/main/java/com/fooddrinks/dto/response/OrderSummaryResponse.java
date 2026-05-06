package com.fooddrinks.dto.response;

import com.fooddrinks.entity.OrderStatus;
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
public class OrderSummaryResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String note;
    private int itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
