package com.fooddrinks.repository;

import com.fooddrinks.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductId(Long productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);
    List<ProductImage> findAllByProductIdAndIsPrimaryTrue(Long productId);
}
