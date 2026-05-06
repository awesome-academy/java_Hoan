package com.fooddrinks.controller.api;

import com.fooddrinks.common.ApiResponse;
import com.fooddrinks.dto.request.AddCartItemRequest;
import com.fooddrinks.dto.request.UpdateCartItemRequest;
import com.fooddrinks.dto.response.CartResponse;
import com.fooddrinks.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/cart — view current cart
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.getCart(userDetails.getUsername())));
    }

    // POST /api/cart/items — add item (or increase qty if already in cart)
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to cart",
                cartService.addItem(userDetails.getUsername(), request)));
    }

    // PUT /api/cart/items/{productId} — set quantity for an existing cart item
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cart item updated",
                cartService.updateItem(userDetails.getUsername(), productId, request)));
    }

    // DELETE /api/cart/items/{productId} — remove one item from cart
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart",
                cartService.removeItem(userDetails.getUsername(), productId)));
    }

    // DELETE /api/cart — clear entire cart
    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Cart cleared",
                cartService.clearCart(userDetails.getUsername())));
    }
}
