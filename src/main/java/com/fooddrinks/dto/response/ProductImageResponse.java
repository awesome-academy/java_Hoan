package com.fooddrinks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
