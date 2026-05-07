package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RatingResponse {

    private Long id;
    private Long productId;
    private String productName;
    /** Average rating of the product after this save — reflects the new value */
    private BigDecimal productAverageRating;
    private Long userId;
    /** Reviewer's display name */
    private String userName;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
