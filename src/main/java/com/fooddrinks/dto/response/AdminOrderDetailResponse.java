package com.fooddrinks.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fooddrinks.entity.OrderStatus;

import lombok.Builder;
import lombok.Getter;

/** Full order detail for the admin — includes user info and all items. */
@Getter
@Builder
public class AdminOrderDetailResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String note;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
