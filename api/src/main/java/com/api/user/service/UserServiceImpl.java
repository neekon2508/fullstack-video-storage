package com.api.user.service;

import org.springframework.stereotype.Service;

import com.api.common.exception.BusinessException;
import com.api.common.exception.ErrorCode;
import com.api.user.UserDTO;
import com.api.user.entity.User;
import com.api.user.mapper.UserMapper;
import com.api.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDTO getUserById(Long id) {
        User user =  userRepository.findById(id).orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );
        return userMapper.fromUser(user);
    }

}
