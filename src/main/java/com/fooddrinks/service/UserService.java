package com.fooddrinks.service;

import com.fooddrinks.dto.request.UpdateProfileRequest;
import com.fooddrinks.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    // --- Admin methods ---

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(Long id);

    /** Flip isActive: active → inactive or inactive → active. */
    UserResponse toggleActive(Long id);
}

