package com.fooddrinks.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fooddrinks.entity.OrderStatus;

import lombok.Builder;
import lombok.Getter;

/** Lightweight order row for the admin order list table. */
@Getter
@Builder
public class AdminOrderSummaryResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private int itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
