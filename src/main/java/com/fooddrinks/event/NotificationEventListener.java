package com.fooddrinks.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fooddrinks.service.impl.EmailNotificationService;
import com.fooddrinks.service.impl.SlackNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens for domain events and dispatches notifications.
 *
 * Key design choices:
 * 
 * {@code @TransactionalEventListener(AFTER_COMMIT)} — the event is delivered
 * only
 * after the DB transaction has committed successfully. This guarantees we never
 * send a Slack/Email for an order that was ultimately rolled back.
 * {@code @Async} — notifications run on a worker thread so the HTTP request
 * thread
 * (and the user's response) is not blocked by Slack/SMTP latency.
 *
 * Because this bean is a separate {@code @Component}, the Spring proxy
 * correctly
 * intercepts both {@code @Async} and {@code @TransactionalEventListener}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SlackNotificationService slackService;
    private final EmailNotificationService emailService;

    /**
     * Handles {@link OrderPlacedEvent}: sends Slack alert + admin email.
     *
     * Runs asynchronously after the placing transaction commits.
     * Any exception thrown here is swallowed by the async executor and logged;
     * it will NOT roll back the order or affect the HTTP response.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.debug("Dispatching notifications for order #{}", event.getOrderId());
        slackService.sendOrderNotification(event);
        emailService.sendOrderNotification(event);
    }
}
