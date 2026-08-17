package com.api.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {

    private String apiKey;

    private String username;

    private String email;

    private String fullName;

    private String avatar;

    private String signature;

    private String role;

    private String phone;

    private Integer tenantId;

    private List<String> func;
}
