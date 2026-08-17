package com.api.common.interceptor;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.auth.service.UserDetailsImpl;
import backend.common.constant.CommonConstants;
import backend.common.exception.ErrorCode;
import backend.common.model.CommonResponse;
import backend.common.util.SecurityUtil;
import backend.common.util.ValidateUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        UserDetailsImpl user = SecurityUtil.getUserDetails();
        List<String> functions = user.getFunctions();
        String uri = request.getServletPath();

        if (ValidateUtil.isEmpty(functions)
                || !functions.contains(uri)) {
            log.error("Inaccessible Api, Access url is " + uri);
            responseWriter(request, response, ErrorCode.NOT_AUTHORIZED_EXCEPTION.getCode());
            return false;
        }
        return true;
    }

    private void responseWriter(HttpServletRequest request, HttpServletResponse response, String statusCode)
            throws Exception {
        CommonResponse<Object> responseBody = CommonResponse.builder()
                .successOrNot(CommonConstants.NO_FLAG)
                .statusCode(statusCode)
                .data("Exception")
                .build();
        String json = objectMapper.writeValueAsString(responseBody);

        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        response.setStatus(200); // 200으로 처리
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
}
