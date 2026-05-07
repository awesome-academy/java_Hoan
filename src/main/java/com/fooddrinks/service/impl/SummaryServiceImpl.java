package com.fooddrinks.service.impl;

import com.fooddrinks.dto.response.CartItemResponse;
import com.fooddrinks.dto.response.CartResponse;
import com.fooddrinks.dto.response.OrderSummaryResponse;
import com.fooddrinks.dto.response.SummaryResponse;
import com.fooddrinks.entity.Cart;
import com.fooddrinks.entity.CartItem;
import com.fooddrinks.entity.ProductImage;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.CartRepository;
import com.fooddrinks.repository.OrderRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        CartResponse cartResponse = buildCartResponse(user.getId());
        List<OrderSummaryResponse> orderHistory = orderRepository.findSummaryByUserId(user.getId());

        return SummaryResponse.builder()
                .cart(cartResponse)
                .orderHistory(orderHistory)
                .build();
    }

    /**
     * Builds CartResponse from the user's cart without auto-creating one.
     * If the user has no cart yet, returns an empty cart (id=null, no items).
     * This keeps GET /api/summary idempotent — no side effects.
     */
    private CartResponse buildCartResponse(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .map(this::toCartResponse)
                .orElse(CartResponse.builder()
                        .id(null)
                        .items(Collections.emptyList())
                        .totalItems(0)
                        .totalAmount(BigDecimal.ZERO)
                        .build());
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        String primaryImageUrl = item.getProduct().getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productPrice(item.getProduct().getPrice())
                .productImageUrl(primaryImageUrl)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .addedAt(item.getAddedAt())
                .build();
    }
}
