package com.api.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import backend.function.repository.FunctionRepository;
import backend.user.entity.User;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final FunctionRepository functionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + username);
        User user = optUser.get();
        if (user.getStatus() != 1)
            throw new DisabledException("Account is locked: " + username);
        String role = user.getRoleId();
        List<String> functions = functionRepository.findAllByRoleId(role).stream().map(function -> function.getUrl())
                .toList();
        return UserDetailsImpl
                .builder()
                .id(user.getId())
                .username(username)
                .password(user.getPassword())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .signature(user.getSignature())
                .role(user.getRoleId())
                .functions(functions)
                .build();
    }

}
