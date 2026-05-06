package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    /** Current live price — not a snapshot */
    private BigDecimal productPrice;
    /** Primary image URL, null if product has no images */
    private String productImageUrl;
    private Integer quantity;
    /** productPrice × quantity, computed in service layer */
    private BigDecimal subtotal;
    private LocalDateTime addedAt;
}
