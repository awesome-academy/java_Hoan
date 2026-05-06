package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.UpdateProfileRequest;
import com.fooddrinks.dto.response.UserResponse;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getProfile(String email) {
        User user = findByEmailOrThrow(email);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmailOrThrow(email);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        // phone and address can be cleared (null = remove)
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        return toResponse(userRepository.save(user));
    }

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .provider(user.getProvider())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
