package com.fooddrinks.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fooddrinks.event.OrderPlacedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends notifications to a Slack channel via Incoming Webhooks.
 *
 * Skips silently when {@code app.slack.webhook-url} is not configured (placeholder value),
 * so the app starts cleanly without a real Slack workspace.
 *
 * Errors are caught and logged as WARN — a failed Slack notification must never propagate
 * back to the caller or disrupt the order flow.
 */
@Slf4j
@Service
public class SlackNotificationService {

    @Value("${app.slack.webhook-url}")
    private String webhookUrl;

    // Reuse a single RestClient instance — it is thread-safe and stateless
    private final RestClient restClient = RestClient.create();

    /**
     * Sends an order-placed alert to the configured Slack channel.
     *
     * @param event snapshot of the placed order (no JPA session required)
     */
    public void sendOrderNotification(OrderPlacedEvent event) {
        if (!isConfigured()) {
            log.debug("Slack webhook not configured — skipping notification for order #{}", event.getOrderId());
            return;
        }

        String text = buildOrderMessage(event);
        post(text, "order #" + event.getOrderId());
    }

    // private helpers

    private String buildOrderMessage(OrderPlacedEvent event) {
        return String.format(
                ":shopping_bags: *New Order #%d* from `%s`%n" +
                ">Amount: *%s* | Items: *%d*",
                event.getOrderId(),
                event.getUserEmail(),
                event.getTotalAmount(),
                event.getItems().size());
    }

    private void post(String text, String context) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Slack notification sent for {}", context);
        } catch (Exception e) {
            // Notification failures must not affect the order flow
            log.warn("Slack notification failed for {}: {}", context, e.getMessage());
        }
    }

    /**
     * Returns {@code true} only when a real webhook URL has been provided.
     * Placeholder value {@code YOUR_SLACK_WEBHOOK_URL} is treated as unconfigured.
     */
    private boolean isConfigured() {
        return webhookUrl != null
                && !webhookUrl.isBlank()
                && !webhookUrl.startsWith("YOUR_");
    }
}
