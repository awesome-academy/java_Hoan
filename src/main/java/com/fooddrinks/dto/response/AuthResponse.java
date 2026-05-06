package com.fooddrinks.dto.response;

import com.fooddrinks.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType; // "Bearer"
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}
