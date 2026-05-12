package com.fooddrinks.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fooddrinks.common.ApiResponse;
import com.fooddrinks.dto.request.RatingRequest;
import com.fooddrinks.dto.response.RatingResponse;
import com.fooddrinks.service.RatingService;
import com.fooddrinks.util.ApiPaths;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Rating endpoints:
 * POST /api/ratings — authenticated: create or update own rating
 * GET /api/products/{productId}/ratings — public: list ratings for a product
 *
 * The GET endpoint deliberately lives under /api/products/** so it falls under
 * the
 * existing "GET /api/products/** → permitAll()" security rule without any
 * changes.
 */
@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // POST /api/ratings — upsert rating (create if first time, update otherwise)
    @PostMapping(ApiPaths.Ratings.URL)
    public ResponseEntity<ApiResponse<RatingResponse>> createOrUpdate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rating saved",
                ratingService.createOrUpdate(userDetails.getUsername(), request)));
    }

    // GET /api/products/{productId}/ratings — list ratings for a product (public)
    @GetMapping(ApiPaths.Products.URL + "/{productId}/ratings")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.getByProduct(productId)));
    }
}
