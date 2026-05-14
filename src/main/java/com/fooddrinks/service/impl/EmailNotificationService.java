package com.fooddrinks.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.fooddrinks.entity.Order;
import com.fooddrinks.entity.OrderStatus;
import com.fooddrinks.event.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends plain-text email notifications via {@link JavaMailSender}.
 *
 * Two notification types:
 * {@link #sendOrderNotification} — fired for every new order (triggered via
 * event)
 * {@link #sendMonthlyReport} — fired by the monthly scheduler
 *
 * Skips silently when {@code spring.mail.username} is a placeholder, so the app
 * starts
 * cleanly without real SMTP credentials.
 *
 * {@link MailException} is caught and logged as WARN — a failed email must
 * never
 * propagate back to the caller or disrupt the order flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    /** Sender address — doubles as the "is configured?" guard. */
    @Value("${spring.mail.username}")
    private String mailFrom;

    /** Notification recipient — separate from app.admin.email (the DB account). */
    @Value("${app.notification.recipient-email}")
    private String recipientEmail;

    // Public API

    /**
     * Sends an order-placed alert to the admin inbox.
     *
     * @param event snapshot of the placed order (no JPA session required)
     */
    public void sendOrderNotification(OrderPlacedEvent event) {
        if (!isConfigured()) {
            log.debug("Mail not configured — skipping notification for order #{}", event.getOrderId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);
        message.setSubject("New Order #" + event.getOrderId() + " from " + event.getUserEmail());
        message.setText(buildOrderEmailText(event));

        send(message, "order #" + event.getOrderId());
    }

    /**
     * Sends a monthly statistics report to the admin inbox.
     * Called by {@link com.fooddrinks.util.MonthlyReportScheduler}.
     *
     * @param from   start of the report range (inclusive)
     * @param to     end of the report range (exclusive)
     * @param orders all orders placed in that range
     */
    public void sendMonthlyReport(LocalDate from, LocalDate to, List<Order> orders) {
        if (!isConfigured()) {
            log.debug("Mail not configured — skipping monthly report for {} to {}", from, to);
            return;
        }

        String rangeLabel = from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — " + to.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);
        message.setSubject("Monthly Report " + rangeLabel);
        message.setText(buildMonthlyReportText(rangeLabel, orders));

        send(message, "monthly report " + rangeLabel);
    }

    // Body builders

    private String buildOrderEmailText(OrderPlacedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("New order received on the Foods & Drinks platform.\n\n");
        sb.append("Order ID   : #").append(event.getOrderId()).append("\n");
        sb.append("Customer   : ").append(event.getUserEmail()).append("\n");
        sb.append("Address    : ").append(event.getShippingAddress()).append("\n");

        if (event.getNote() != null && !event.getNote().isBlank()) {
            sb.append("Note       : ").append(event.getNote()).append("\n");
        }

        sb.append("\nItems:\n");
        for (OrderPlacedEvent.ItemSnapshot item : event.getItems()) {
            sb.append(String.format("  - %-30s x%-3d @ %s = %s%n",
                    item.productName(), item.quantity(), item.productPrice(), item.subtotal()));
        }

        sb.append("\nTotal      : ").append(event.getTotalAmount()).append("\n");
        return sb.toString();
    }

    private String buildMonthlyReportText(String rangeLabel, List<Order> orders) {
        long total = orders.size();
        long pending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long confirmed = orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long delivering = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERING).count();
        long completed = orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        BigDecimal revenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
                "Monthly Report — %s%n%n" +
                        "Total Orders  : %d%n" +
                        "  Pending     : %d%n" +
                        "  Confirmed   : %d%n" +
                        "  Delivering  : %d%n" +
                        "  Completed   : %d%n" +
                        "  Cancelled   : %d%n%n" +
                        "Revenue (Completed): %s%n",
                rangeLabel, total, pending, confirmed, delivering, completed, cancelled, revenue);
    }

    // Helpers

    private void send(SimpleMailMessage message, String context) {
        try {
            mailSender.send(message);
            log.debug("Email sent for {}", context);
        } catch (MailException e) {
            // Notification failures must not affect calling business logic
            log.warn("Email failed for {}: {}", context, e.getMessage());
        }
    }

    /**
     * Returns {@code true} only when a real sender address has been set.
     * The placeholder {@code YOUR_EMAIL@gmail.com} is treated as unconfigured.
     */
    private boolean isConfigured() {
        return mailFrom != null
                && !mailFrom.isBlank()
                && !mailFrom.startsWith("YOUR_");
    }
}
