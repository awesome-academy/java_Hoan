package com.fooddrinks.dto.response;

import com.fooddrinks.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order detail including all order items with price snapshots.
 */
@Getter
@Builder
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String note;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
