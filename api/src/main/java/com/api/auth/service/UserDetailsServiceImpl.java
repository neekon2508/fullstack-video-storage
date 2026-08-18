package com.api.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.api.user.entity.User;
import com.api.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    // private final FunctionRepository functionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + username);
        User user = optUser.get();
        if (user.getIsActive())
            throw new DisabledException("Account is locked: " + username);
        return UserDetailsImpl
                .builder()
                .id(user.getId())
                .username(username)
                .password(user.getPasswordHash())
                .email(user.getEmail())
                .build();
    }

}
