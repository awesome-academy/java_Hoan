package com.fooddrinks.repository;

import com.fooddrinks.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductId(Long productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    List<ProductImage> findAllByProductIdAndIsPrimaryTrue(Long productId);

    // clearAutomatically: evicts cleared rows from persistence context so
    // subsequent
    // reads see the updated state. flushAutomatically: flushes pending changes
    // before
    // running the UPDATE to avoid stale-write conflicts.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId AND pi.isPrimary = true")
    void clearPrimaryImages(@Param("productId") Long productId);
}
