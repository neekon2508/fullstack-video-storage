package com.api.auth.service;

import com.api.auth.dto.LoginRequest;
import com.api.auth.dto.LoginResponse;

public interface AuthenService {
    LoginResponse login(LoginRequest login);

    String logout();

    String handleForgotPassword(String username);
}
