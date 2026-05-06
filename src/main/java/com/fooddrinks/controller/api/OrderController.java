package com.fooddrinks.controller.api;

import com.fooddrinks.common.ApiResponse;
import com.fooddrinks.dto.request.PlaceOrderRequest;
import com.fooddrinks.dto.response.OrderResponse;
import com.fooddrinks.dto.response.OrderSummaryResponse;
import com.fooddrinks.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // POST /api/orders — place order from current cart
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Order placed successfully",
                        orderService.placeOrder(userDetails.getUsername(), request)));
    }

    // GET /api/orders — order history (lightweight, no items)
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getHistory(userDetails.getUsername())));
    }

    // GET /api/orders/{id} — full order detail with items
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getById(userDetails.getUsername(), id)));
    }
}
