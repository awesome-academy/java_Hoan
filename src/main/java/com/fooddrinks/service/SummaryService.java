package com.fooddrinks.service;

import com.fooddrinks.dto.response.SummaryResponse;

public interface SummaryService {

    /**
     * Returns the user's current cart and full order history.
     * Cart is auto-created (empty) if the user has never added an item.
     */
    SummaryResponse getSummary(String email);
}
