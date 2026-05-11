package com.fooddrinks.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddrinks.dto.request.PlaceOrderRequest;
import com.fooddrinks.dto.response.AdminOrderDetailResponse;
import com.fooddrinks.dto.response.AdminOrderSummaryResponse;
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

        // Lock the cart row (PESSIMISTIC_WRITE / SELECT FOR UPDATE) before reading its
        // items.
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
            // This serializes concurrent orders for the same product, preventing
            // overselling.
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

    // --- Admin methods ---

    /**
     * Valid order status transitions.
     * COMPLETED and CANCELLED are terminal — once set, no further changes allowed.
     */
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = java.util.Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED),
            OrderStatus.DELIVERING, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminOrderSummaryResponse> getAllOrdersForAdmin(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toAdminSummary);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminOrderDetailResponse getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toAdminDetail(order);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminOrderDetailResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(newStatus)) {
            throw new BadRequestException(
                    "Cannot transition order #" + orderId + " from " + current + " to " + newStatus
                            + ". Allowed: " + (allowed.isEmpty() ? "none (terminal state)" : allowed));
        }

        order.setStatus(newStatus);
        return toAdminDetail(orderRepository.save(order));
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

    private AdminOrderSummaryResponse toAdminSummary(Order order) {
        return AdminOrderSummaryResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .userFullName(order.getUser().getFullName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                // items are NOT eagerly loaded in findAll(Pageable) — we avoid loading the
                // collection for list queries; itemCount is omitted (shown as 0) for now.
                // Use getOrderByIdForAdmin for full item detail.
                .itemCount(0)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private AdminOrderDetailResponse toAdminDetail(Order order) {
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

        return AdminOrderDetailResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .userFullName(order.getUser().getFullName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
