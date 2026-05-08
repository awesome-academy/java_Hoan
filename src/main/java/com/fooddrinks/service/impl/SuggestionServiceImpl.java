package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.SuggestionRequest;
import com.fooddrinks.dto.response.SuggestionResponse;
import com.fooddrinks.entity.Suggestion;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.SuggestionRepository;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.SuggestionService;
import lombok.RequiredArgsConstructor;
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
}
