package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.PlaceOrderRequest;
import com.fooddrinks.dto.response.OrderItemResponse;
import com.fooddrinks.dto.response.OrderResponse;
import com.fooddrinks.dto.response.OrderSummaryResponse;
import com.fooddrinks.entity.Cart;
import com.fooddrinks.entity.CartItem;
import com.fooddrinks.entity.Order;
import com.fooddrinks.entity.OrderItem;
import com.fooddrinks.entity.OrderStatus;
import com.fooddrinks.entity.Product;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.CartRepository;
import com.fooddrinks.repository.OrderRepository;
import com.fooddrinks.repository.ProductRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = findUserOrThrow(email);

        // Lock the cart row (PESSIMISTIC_WRITE / SELECT FOR UPDATE) before reading its items.
        // This prevents two concurrent requests from the same user (e.g. double-click
        // "Place Order") from both reading the same cart, decrementing stock twice,
        // and creating duplicate orders before either transaction clears the cart.
        Cart cart = cartRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        // Build order header
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : items) {
            Long productId = cartItem.getProduct().getId();
            int qty = cartItem.getQuantity();

            // Acquire a row-level pessimistic write lock (SELECT ... FOR UPDATE).
            // This serializes concurrent orders for the same product, preventing overselling.
            Product product = productRepository.findActiveByIdWithLock(productId)
                    .orElseThrow(() -> new BadRequestException(
                            "Product is no longer available (id=" + productId + ")"));

            if (product.getStockQuantity() < qty) {
                throw new BadRequestException(
                        "Insufficient stock for \"" + product.getName() + "\""
                        + ". Available: " + product.getStockQuantity()
                        + ", requested: " + qty);
            }

            // Decrement stock atomically under the lock
            product.setStockQuantity(product.getStockQuantity() - qty);

            // Snapshot: capture name and price at time of order
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(qty);
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        order.getItems().addAll(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Clear the cart after a successful order (orphanRemoval = true handles DELETE)
        cart.getItems().clear();
        cartRepository.save(cart);

        // TODO Day 9: fire OrderPlacedEvent here for Slack / Email notifications

        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getHistory(String email) {
        User user = findUserOrThrow(email);
        // findSummaryByUserId uses COUNT(i) in the DB — avoids loading items collection
        // just to call size(), keeping the history endpoint truly lightweight.
        return orderRepository.findSummaryByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(String email, Long orderId) {
        User user = findUserOrThrow(email);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Ownership check — users may only view their own orders
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        return toOrderResponse(order);
    }

    // --- helpers ---

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productPrice(item.getProductPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
