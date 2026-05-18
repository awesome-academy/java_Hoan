package com.fooddrinks.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fooddrinks.entity.Order;
import com.fooddrinks.repository.OrderRepository;
import com.fooddrinks.service.impl.EmailNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends a monthly order-statistics report to the admin at the start of each
 * month.
 *
 * The cron fires at 08:00 on the 1st of every month. It queries all orders from
 * the previous month, builds a plain-text summary, and sends it via
 * {@link EmailNotificationService#sendMonthlyReport}.
 *
 * The method is {@code @Transactional(readOnly = true)} so that the
 * {@code findByCreatedAtBetween} query runs inside a proper Hibernate session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private final OrderRepository orderRepository;
    private final EmailNotificationService emailService;

    /**
     * Cron: {@code 0 0 8 1 * *} → 08:00 AM on the 1st day of every month.
     *
     * <p>The time range is a rolling calendar-date window using start-of-day
     * boundaries (half-open interval {@code [today - 1 month, today)}):
     * <pre>
     *   start = today - 1 month at 00:00:00  (inclusive)
     *   end   = today           at 00:00:00  (exclusive — orders placed today not included)
     * </pre>
     *
     * @return {@code true} if the report email was sent; {@code false} if mail is not configured
     */
    @Scheduled(cron = "0 0 8 1 * *")
    @Transactional(readOnly = true)
    public boolean sendMonthlyReport() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusMonths(1);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = today.atStartOfDay(); // exclusive

        log.info("Generating monthly report for range: {} — {}", start, end);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        log.info("Monthly report: {} orders found", orders.size());

        boolean sent = emailService.sendMonthlyReport(from, today, orders);
        if (!sent) {
            log.warn("Monthly report skipped — mail is not configured");
        }
        return sent;
    }
}
