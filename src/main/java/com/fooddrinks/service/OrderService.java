package com.fooddrinks.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fooddrinks.dto.request.PlaceOrderRequest;
import com.fooddrinks.dto.response.AdminOrderDetailResponse;
import com.fooddrinks.dto.response.AdminOrderSummaryResponse;
import com.fooddrinks.dto.response.OrderResponse;
import com.fooddrinks.dto.response.OrderSummaryResponse;
import com.fooddrinks.entity.OrderStatus;

public interface OrderService {

    /**
     * Place an order from the current user's cart.
     * Decrements stock for each product atomically (pessimistic lock).
     * Clears the cart on success.
     */
    OrderResponse placeOrder(String email, PlaceOrderRequest request);

    /**
     * Returns a lightweight summary list of all orders for the user (newest first).
     */
    List<OrderSummaryResponse> getHistory(String email);

    /**
     * Returns the full order detail including items.
     * Throws ResourceNotFoundException if the order doesn't belong to this user.
     */
    OrderResponse getById(String email, Long orderId);

    // --- Admin methods ---

    /** List all orders (any status) with user info, newest first. */
    Page<AdminOrderSummaryResponse> getAllOrdersForAdmin(Pageable pageable);

    /**
     * Returns the set of statuses that can be transitioned to from the given
     * status.
     */
    Set<OrderStatus> getAllowedTransitions(OrderStatus current);

    /** Get full order detail (items + user info) regardless of status. */
    AdminOrderDetailResponse getOrderByIdForAdmin(Long orderId);

    /**
     * Update the status of an order.
     * Terminal states (COMPLETED, CANCELLED) cannot be changed.
     * Throws BadRequestException for invalid transitions.
     */
    AdminOrderDetailResponse updateOrderStatus(Long orderId, OrderStatus newStatus);
}
