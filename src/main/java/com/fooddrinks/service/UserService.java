package com.fooddrinks.service;

import com.fooddrinks.dto.request.UpdateProfileRequest;
import com.fooddrinks.dto.response.UserResponse;

public interface UserService {

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);
}
