package com.fooddrinks.service;

import com.fooddrinks.dto.request.LoginRequest;
import com.fooddrinks.dto.request.RegisterRequest;
import com.fooddrinks.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
