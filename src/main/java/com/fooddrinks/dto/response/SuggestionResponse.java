package com.fooddrinks.dto.response;

import com.fooddrinks.entity.SuggestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SuggestionResponse {

    private Long id;
    private String name;
    private String description;
    private SuggestionStatus status;
    private LocalDateTime createdAt;
}
