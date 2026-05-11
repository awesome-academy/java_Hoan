package com.fooddrinks.service;

import com.fooddrinks.dto.request.SuggestionRequest;
import com.fooddrinks.dto.response.SuggestionResponse;

import java.util.List;

public interface SuggestionService {

    /** Submit a new product suggestion. Status defaults to PENDING. */
    SuggestionResponse create(String email, SuggestionRequest request);

    /** List all suggestions submitted by the current user (newest first). */
    List<SuggestionResponse> getMyList(String email);
}
