package com.fooddrinks.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fooddrinks.dto.request.SuggestionRequest;
import com.fooddrinks.dto.response.AdminSuggestionResponse;
import com.fooddrinks.dto.response.SuggestionResponse;
import com.fooddrinks.entity.SuggestionStatus;

public interface SuggestionService {

    /** Submit a new product suggestion. Status defaults to PENDING. */
    SuggestionResponse create(String email, SuggestionRequest request);

    /** List all suggestions submitted by the current user (newest first). */
    List<SuggestionResponse> getMyList(String email);

    // --- Admin methods ---

    /** List all suggestions (any status) with user info, newest first. */
    Page<AdminSuggestionResponse> getAllSuggestionsForAdmin(Pageable pageable);

    /** Get suggestion detail including user info, regardless of status. */
    AdminSuggestionResponse getSuggestionByIdForAdmin(Long id);

    /** Update suggestion status. Admin can set any status on any suggestion. */
    AdminSuggestionResponse updateSuggestionStatus(Long id, SuggestionStatus newStatus);
}
