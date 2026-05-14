package com.fooddrinks.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddrinks.common.ApiResponse;
import com.fooddrinks.dto.response.SummaryResponse;
import com.fooddrinks.service.SummaryService;
import com.fooddrinks.util.ApiPaths;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiPaths.Summary.URL)
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    // GET /api/summary — current cart + full order history
    @GetMapping
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                summaryService.getSummary(userDetails.getUsername())));
    }
}
