package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.SuggestionRequest;
import com.fooddrinks.dto.response.AdminSuggestionResponse;
import com.fooddrinks.dto.response.SuggestionResponse;
import com.fooddrinks.entity.Suggestion;
import com.fooddrinks.entity.SuggestionStatus;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.SuggestionRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SuggestionResponse create(String email, SuggestionRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Suggestion suggestion = new Suggestion();
        suggestion.setUser(user);
        suggestion.setName(request.getName());
        suggestion.setDescription(request.getDescription());
        // status defaults to PENDING via entity initializer

        return toSuggestionResponse(suggestionRepository.save(suggestion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResponse> getMyList(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return suggestionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSuggestionResponse)
                .toList();
    }

    private SuggestionResponse toSuggestionResponse(Suggestion suggestion) {
        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .name(suggestion.getName())
                .description(suggestion.getDescription())
                .status(suggestion.getStatus())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }

    // --- Admin methods ---

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminSuggestionResponse> getAllSuggestionsForAdmin(Pageable pageable) {
        return suggestionRepository.findAll(pageable).map(this::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminSuggestionResponse getSuggestionByIdForAdmin(Long id) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion", id));
        return toAdminResponse(suggestion);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminSuggestionResponse updateSuggestionStatus(Long id, SuggestionStatus newStatus) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion", id));
        suggestion.setStatus(newStatus);
        return toAdminResponse(suggestionRepository.save(suggestion));
    }

    private AdminSuggestionResponse toAdminResponse(Suggestion suggestion) {
        return AdminSuggestionResponse.builder()
                .id(suggestion.getId())
                .userId(suggestion.getUser().getId())
                .userEmail(suggestion.getUser().getEmail())
                .userFullName(suggestion.getUser().getFullName())
                .name(suggestion.getName())
                .description(suggestion.getDescription())
                .status(suggestion.getStatus())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}
