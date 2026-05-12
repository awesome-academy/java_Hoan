package com.fooddrinks.service;

import com.fooddrinks.dto.response.DashboardStatsResponse;

public interface DashboardService {

    /** Aggregate stats for the admin dashboard panel. */
    DashboardStatsResponse getStats();
}
