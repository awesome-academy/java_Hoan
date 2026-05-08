package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    /** Snapshot of product name at the time of order */
    private String productName;
    /** Snapshot of product price at the time of order */
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
