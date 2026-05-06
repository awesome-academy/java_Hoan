package com.fooddrinks.service.impl;

import com.fooddrinks.dto.request.LoginRequest;
import com.fooddrinks.dto.request.RegisterRequest;
import com.fooddrinks.dto.response.AuthResponse;
import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.Role;
import com.fooddrinks.entity.User;
import com.fooddrinks.exception.ConflictException;
import com.fooddrinks.exception.UnauthorizedException;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.service.AuthService;
import com.fooddrinks.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.USER);
        user.setProvider(Provider.LOCAL);
        user.setEmailVerified(false);
        user.setIsActive(true);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // OAuth2 users cannot log in with a password
        if (user.getProvider() != Provider.LOCAL) {
            throw new UnauthorizedException(
                    "This account was created with " + user.getProvider().name().toLowerCase() + ". Please sign in using that provider.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
