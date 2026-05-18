package com.fooddrinks.controller.admin;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fooddrinks.util.AdminPaths;
import com.fooddrinks.util.MonthlyReportScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only actions for notification management.
 *
 * Currently exposes a single "trigger monthly report now" action for
 * dev/ops use — useful for testing email delivery without waiting for the
 * scheduled cron to fire.
 *
 * Secured by the admin filter chain (@Order(1)) which requires an active
 * admin session; no JWT needed.
 */
@Slf4j
@Controller
@RequestMapping(AdminPaths.ROOT + "/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final MonthlyReportScheduler monthlyReportScheduler;

    /**
     * Minimum interval between manual triggers — prevents accidental email spam.
     */
    private static final long MIN_INTERVAL_SECONDS = 60;

    /** Tracks the last successful trigger time across all admin sessions. */
    private final AtomicReference<Instant> lastTriggered = new AtomicReference<>(Instant.EPOCH);

    /**
     * Manually triggers the monthly report job and redirects back to the dashboard
     * with a flash message indicating success or failure.
     *
     * POST to avoid accidental triggering via browser back/refresh.
     * Rate-limited to once per {@value #MIN_INTERVAL_SECONDS} seconds to prevent
     * email spam.
     */
    @PostMapping("/monthly-report/trigger")
    public String triggerMonthlyReport(RedirectAttributes redirectAttributes) {
        Instant now = Instant.now();
        Instant last = lastTriggered.get();
        long secondsSinceLast = now.getEpochSecond() - last.getEpochSecond();

        if (secondsSinceLast < MIN_INTERVAL_SECONDS) {
            long wait = MIN_INTERVAL_SECONDS - secondsSinceLast;
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Monthly report was triggered recently. Please wait " + wait + " second(s).");
            return "redirect:" + AdminPaths.Dashboard.URL;
        }

        try {
            boolean sent = monthlyReportScheduler.sendMonthlyReport();
            lastTriggered.set(now);
            if (sent) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Monthly report triggered — check your inbox.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Monthly report was not sent — mail is not configured on this server.");
            }
        } catch (Exception e) {
            // Log full error internally; expose only a generic message to the UI
            // to avoid leaking internal details (SMTP config, class names, etc.)
            log.error("Manual monthly report trigger failed", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to send monthly report. Check application logs for details.");
        }
        return "redirect:" + AdminPaths.Dashboard.URL;
    }
}
