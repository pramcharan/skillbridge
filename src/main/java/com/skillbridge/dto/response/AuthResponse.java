package com.skillbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private Long   userId;
    private String name;
    private String email;
    private boolean onboardingComplete;
}