package com.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private AuthResponse authResponse;
    private String refreshToken;
}
