package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Aggregated view of a user's current state:
 * their active cart and full order history.
 */
@Getter
@Builder
public class SummaryResponse {

    private CartResponse cart;
    private List<OrderSummaryResponse> orderHistory;
}
