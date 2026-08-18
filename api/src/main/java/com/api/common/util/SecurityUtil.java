package com.api.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.api.auth.service.UserDetailsImpl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtil {

    public UserDetailsImpl getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        return user;
    }
}
