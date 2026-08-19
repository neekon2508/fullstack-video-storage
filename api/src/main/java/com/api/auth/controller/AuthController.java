package com.api.auth.controller;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.auth.dto.AuthResponse;
import com.api.auth.dto.LoginRequest;
import com.api.auth.dto.LoginResponse;
import com.api.auth.service.AuthenService;
import com.api.common.constant.CommonConstants;
import com.api.common.model.CommonResponse;
import com.api.common.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
@RequestMapping("/auth")
public class AuthController {

    private final AuthenService authService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Đăng nhập")
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(req);
        Cookie cookie = jwtUtil.createRefreshTokenCookie(loginResponse.getRefreshToken());
        response.addCookie(cookie);

        return ResponseEntity.ok(CommonResponse.success(loginResponse.getAuthResponse()));
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping(value = "/logout")
    public ResponseEntity<CommonResponse<String>> logout(
            @CookieValue(value = "MOVIE_REFRESH-TOKEN", required = true) String refreshToken,
            HttpServletResponse response) {
        ResponseCookie clearCookie = ResponseCookie.from("MOVIE_REFRESH-TOKEN", "")
                .httpOnly(true)
                .path("/api/movie")
                .maxAge(0)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(
                CommonResponse.success(authService.logout()));
    }

    @PostMapping(value = "/refresh")
    public ResponseEntity<CommonResponse<AuthResponse>> refresh(
            @CookieValue(value = "MOVIE_REFRESH-TOKEN", required = true) String refreshToken) {
        return ResponseEntity.ok(CommonResponse.success(authService.refresh(refreshToken)));
    }
    // @Operation(summary = "Quên mật khẩu")
    // @GetMapping(value = "/forgotPassword/{username}")
    // public ResponseEntity<CommonResponse<String>>
    // forgotPassword(@PathVariable("username") String username) {
    // return ResponseEntity.ok(
    // CommonResponse.<String>builder()
    // .successOrNot(CommonConstants.YES_FLAG)
    // .statusCode(ErrorCode.SUCCESS.getCode())
    // .data(authService.handleForgotPassword(username))
    // .build());
    // }
}
