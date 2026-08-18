package com.api.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

import com.api.user.UserDTO;
import com.api.user.entity.User;

@Mapper
public interface UserMapper {

    @Mappings({
    })
    UserDTO fromUser(User user);
}