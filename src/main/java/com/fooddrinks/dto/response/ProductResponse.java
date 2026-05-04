package com.fooddrinks.dto.response;

import com.fooddrinks.entity.ProductType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private ProductType type;
    private Integer stockQuantity;
    private BigDecimal averageRating;
    private Boolean isActive;
    private Long categoryId;
    private String categoryName;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
