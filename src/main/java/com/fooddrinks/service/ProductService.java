package com.fooddrinks.service;

import com.fooddrinks.dto.request.ProductRequest;
import com.fooddrinks.dto.response.ProductResponse;
import com.fooddrinks.entity.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface ProductService {

    Page<ProductResponse> getAll(String letter, ProductType type, Long categoryId,
                                 BigDecimal minPrice, BigDecimal maxPrice,
                                 BigDecimal minRating, Pageable pageable);

    ProductResponse getById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductResponse addImage(Long productId, MultipartFile file, boolean isPrimary);

    void deleteImage(Long productId, Long imageId);

    // --- Admin methods ---

    /** List ALL products (including inactive) for admin management. */
    Page<ProductResponse> getAllForAdmin(Pageable pageable);

    /** Admin update — works on both active and inactive products. */
    ProductResponse updateForAdmin(Long id, ProductRequest request);

    /** Restore a soft-deleted product (set isActive = true). */
    ProductResponse restoreProduct(Long id);

    /** Get any product by id regardless of isActive flag (for admin edit). */
    ProductResponse getByIdForAdmin(Long id);
}

