package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    /** Total number of items (sum of quantities) */
    private int totalItems;
    /** Sum of all item subtotals at current prices */
    private BigDecimal totalAmount;
}
