package com.api.auth.service;

import java.util.Optional;
import java.util.Random;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.api.auth.dto.LoginRequest;
import com.api.auth.dto.LoginResponse;
import com.api.common.exception.BusinessException;
import com.api.common.exception.ErrorCode;
import com.api.common.util.JwtUtil;
import com.api.user.entity.User;
import com.api.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenServiceImpl implements AuthenService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest login) {
        String username = login.getUsername();
        String password = login.getPassword();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtil.generateAccessToken(user);

        return LoginResponse.builder()
                .apiKey(token)
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .func(user.getFunctions())
                .phone(user.getPhone())
                .role(user.getRole())
                .signature(user.getSignature())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public String logout() {
        return "Đăng xuất thành công";
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
