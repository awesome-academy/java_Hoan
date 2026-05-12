package com.fooddrinks.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddrinks.common.ApiResponse;
import com.fooddrinks.dto.request.SuggestionRequest;
import com.fooddrinks.dto.response.SuggestionResponse;
import com.fooddrinks.service.SuggestionService;
import com.fooddrinks.util.ApiPaths;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiPaths.Suggestions.URL)
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    // POST /api/suggestions — submit a new product suggestion
    @PostMapping
    public ResponseEntity<ApiResponse<SuggestionResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SuggestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Suggestion submitted",
                        suggestionService.create(userDetails.getUsername(), request)));
    }

    // GET /api/suggestions — list own suggestions (newest first)
    @GetMapping
    public ResponseEntity<ApiResponse<List<SuggestionResponse>>> getMyList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                suggestionService.getMyList(userDetails.getUsername())));
    }
}
