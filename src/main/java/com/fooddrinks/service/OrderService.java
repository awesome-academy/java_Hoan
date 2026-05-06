package com.fooddrinks.service;

import com.fooddrinks.dto.request.PlaceOrderRequest;
import com.fooddrinks.dto.response.OrderResponse;
import com.fooddrinks.dto.response.OrderSummaryResponse;

import java.util.List;

public interface OrderService {

    /**
     * Place an order from the current user's cart.
     * Decrements stock for each product atomically (pessimistic lock).
     * Clears the cart on success.
     */
    OrderResponse placeOrder(String email, PlaceOrderRequest request);

    /** Returns a lightweight summary list of all orders for the user (newest first). */
    List<OrderSummaryResponse> getHistory(String email);

    /**
     * Returns the full order detail including items.
     * Throws ResourceNotFoundException if the order doesn't belong to this user.
     */
    OrderResponse getById(String email, Long orderId);
}
