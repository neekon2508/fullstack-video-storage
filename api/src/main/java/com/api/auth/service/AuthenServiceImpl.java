package com.api.auth.service;

import java.util.Optional;
import java.util.Random;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.api.auth.dto.AuthResponse;
import com.api.auth.dto.LoginRequest;
import com.api.auth.dto.LoginResponse;
import com.api.common.exception.BusinessException;
import com.api.common.exception.ErrorCode;
import com.api.common.exception.InvalidRefreshTokenException;
import com.api.common.util.JwtUtil;
import com.api.user.UserDTO;
import com.api.user.entity.User;
import com.api.user.repository.UserRepository;
import com.api.user.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenServiceImpl implements AuthenService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest login) {
        String username = login.getUsername();
        String password = login.getPassword();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
        AuthResponse authResponse = AuthResponse.builder()
            .accessToken(accessToken)
            .user(userDTO)
            .build();
        return new LoginResponse(authResponse, refreshToken);
    }

    @Override
    public String logout() {
        return "Đăng xuất thành công";
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken,true))
            throw new InvalidRefreshTokenException("Invalid refresh token");
        Claims claims = jwtUtil.extractClaims(refreshToken, true);
        Long userId = Long.valueOf(claims.getId());
        String username = claims.getSubject();
        String accessToken = jwtUtil.generateAccessToken(userId, username);
        return AuthResponse.builder()
            .accessToken(accessToken)
            .user(new UserDTO(userId, username))
            .build();
    }

    // @Override
    // @Transactional
    // public String handleForgotPassword(String username) {
    // Optional<User> userOpt = userRepository.findByUsername(username);
    // if (userOpt.isEmpty())
    // throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    // User user = userOpt.get();
    // String mail = user.getEmail();
    // String newPass = String.format("%06d", new Random().nextInt(999999));
    // UserDTO userDto = UserDTO.fromEntity(user);
    // userDto.setPassword(newPass);
    // int count = userRepository.updateUser(userDto);
    // if (count == 0)
    // throw new BusinessException(ErrorCode.USER_UPDATE_FAIL.getMessage(),
    // ErrorCode.USER_UPDATE_FAIL.getCode());
    // emailService.sendForgotPasswordEmail(mail, username, newPass);
    // return "Mật khẩu mới đã được gửi về email: " + mail;
    // }

}
