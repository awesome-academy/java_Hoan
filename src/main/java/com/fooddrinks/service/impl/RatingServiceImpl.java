package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.RatingRequest;
import com.fooddrinks.dto.response.RatingResponse;
import com.fooddrinks.entity.Product;
import com.fooddrinks.entity.Rating;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.ProductRepository;
import com.fooddrinks.repository.RatingRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RatingResponse createOrUpdate(String email, RatingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // PESSIMISTIC_WRITE lock on the product row:
        // Serializes concurrent rating saves for the same product, ensuring
        // the averageRating recalculation always reads a consistent total.
        // Only active products can be rated (consistent with the "browse active only"
        // UX).
        Product product = productRepository.findActiveByIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        // Upsert: find existing rating or create new one
        Rating rating = ratingRepository
                .findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(Rating::new);

        rating.setUser(user);
        rating.setProduct(product);
        rating.setScore(request.getScore());
        rating.setComment(request.getComment());
        ratingRepository.save(rating);

        // Recalculate averageRating AFTER save so the new score is included in AVG().
        // Rounds to 1 decimal place (e.g. 4.33 → 4.3).
        Double avg = ratingRepository.calculateAverageRating(product.getId());
        BigDecimal newAverage = avg != null
                ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        product.setAverageRating(newAverage);
        // productRepository.save() not needed — product is already a managed entity
        // within the same @Transactional, dirty-checking will flush the update.

        return toRatingResponse(rating, newAverage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getByProduct(Long productId) {
        // findById instead of existsById: loads product into the persistence context
        // (PC).
        // When the ratings query runs next, accessing r.getProduct() in the mapping
        // will resolve from the PC cache — no extra query, even though product is LAZY.
        // This turns a potential "1 extra query" into 0 extra queries.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        return ratingRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(r -> toRatingResponse(r, product.getAverageRating()))
                .toList();
    }

    private RatingResponse toRatingResponse(Rating rating, BigDecimal productAverageRating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .productId(rating.getProduct().getId())
                .productName(rating.getProduct().getName())
                .productAverageRating(productAverageRating)
                .userId(rating.getUser().getId())
                .userName(rating.getUser().getFullName())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
