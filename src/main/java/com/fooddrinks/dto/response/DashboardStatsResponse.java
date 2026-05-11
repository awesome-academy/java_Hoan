package com.fooddrinks.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/** Aggregated stats for the admin dashboard. */
@Getter
@Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalProducts;
    private long activeProducts;
    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;
    /** Sum of totalAmount for all COMPLETED orders. */
    private BigDecimal totalRevenue;
    private long totalSuggestions;
    private long pendingSuggestions;
}
