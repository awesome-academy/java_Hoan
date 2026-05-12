package com.fooddrinks.dto.response;

import java.time.LocalDateTime;

import com.fooddrinks.entity.SuggestionStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Suggestion DTO for admin — includes user info that user-facing
 * SuggestionResponse omits.
 */
@Getter
@Builder
public class AdminSuggestionResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String name;
    private String description;
    private SuggestionStatus status;
    private LocalDateTime createdAt;
}
