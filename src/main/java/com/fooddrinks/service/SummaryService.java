package com.fooddrinks.service;

import com.fooddrinks.dto.response.SummaryResponse;

public interface SummaryService {

    /**
     * Returns the user's current cart and full order history.
     * If the user has no cart yet, an empty cart (id=null, no items) is returned.
     * This operation is side-effect free — no cart is created.
     */
    SummaryResponse getSummary(String email);
}
