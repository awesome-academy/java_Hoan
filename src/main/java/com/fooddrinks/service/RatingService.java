package com.fooddrinks.service;

import com.fooddrinks.dto.request.RatingRequest;
import com.fooddrinks.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {

    /**
     * Create or update the current user's rating for a product.
     * Each user can rate a product only once — calling this again updates the score/comment.
     * Recalculates and persists the product's averageRating in the same transaction.
     */
    RatingResponse createOrUpdate(String email, RatingRequest request);

    /**
     * List all ratings for a product (public, newest first).
     * Throws ResourceNotFoundException if the product does not exist.
     */
    List<RatingResponse> getByProduct(Long productId);
}
