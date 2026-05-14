package com.fooddrinks.dto.request;

import java.math.BigDecimal;

import com.fooddrinks.dto.response.ProductResponse;
import com.fooddrinks.entity.ProductType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    /**
     * Creates a ProductRequest pre-populated from an existing product (for edit
     * forms).
     */
    public static ProductRequest fromResponse(ProductResponse r) {
        ProductRequest req = new ProductRequest();
        req.setName(r.getName());
        req.setDescription(r.getDescription());
        req.setPrice(r.getPrice());
        req.setType(r.getType());
        req.setStockQuantity(r.getStockQuantity());
        req.setCategoryId(r.getCategoryId());
        return req;
    }

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Product type is required")
    private ProductType type;

    @Min(value = 0, message = "Stock quantity must be >= 0")
    private Integer stockQuantity = 0;

    private Long categoryId;
}
