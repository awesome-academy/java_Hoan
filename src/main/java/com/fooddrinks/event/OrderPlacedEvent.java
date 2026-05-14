package com.fooddrinks.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published by {@link com.fooddrinks.service.impl.OrderServiceImpl} immediately
 * after
 * a new order is successfully persisted.
 *
 * Carries a snapshot of all notification-relevant data so that async listeners
 * (which run in a different thread after the DB transaction has committed) do
 * NOT
 * need to access the JPA session or risk a {@code LazyInitializationException}.
 *
 * Consumed by {@link NotificationEventListener} which sends Slack + Email
 * alerts.
 */
public class OrderPlacedEvent {

    private final long orderId;
    private final String userEmail;
    private final BigDecimal totalAmount;
    private final String shippingAddress;
    private final String note;
    private final List<ItemSnapshot> items;

    public OrderPlacedEvent(long orderId, String userEmail, BigDecimal totalAmount,
            String shippingAddress, String note, List<ItemSnapshot> items) {
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.items = List.copyOf(items); // defensive copy — immutable snapshot
    }

    public long getOrderId() {
        return orderId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public List<ItemSnapshot> getItems() {
        return items;
    }

    /**
     * Immutable snapshot of a single order line at the time the order was placed.
     */
    public record ItemSnapshot(
            String productName,
            BigDecimal productPrice,
            int quantity,
            BigDecimal subtotal) {
    }
}
