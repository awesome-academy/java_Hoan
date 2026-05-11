package com.fooddrinks.repository;

import com.fooddrinks.entity.Rating;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.product.id = :productId")
    Double calculateAverageRating(@Param("productId") Long productId);

    /**
     * List all ratings for a product, newest first.
     * @EntityGraph eagerly loads the user association in a single JOIN FETCH
     * to avoid N+1 queries when accessing user.fullName in the response mapping.
     */
    @EntityGraph(attributePaths = {"user"})
    List<Rating> findByProductIdOrderByCreatedAtDesc(Long productId);
}

