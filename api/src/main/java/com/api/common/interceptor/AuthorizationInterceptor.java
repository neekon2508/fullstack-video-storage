package com.api.common.interceptor;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.api.annotation.RequirePermission;
import com.api.auth.service.UserDetailsImpl;
import com.api.common.constant.CommonConstants;
import com.api.common.exception.ErrorCode;
import com.api.common.model.CommonResponse;
import com.api.common.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        // Không phải request tới controller method (vd static resource) -> bỏ qua
        if (!(handler instanceof HandlerMethod handlerMethod))
            return true;
        // Ưu tiên annotation trên method, fallback annotation trên class
        RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (required == null)
            required = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        // API không khai báo @RequirePermission -> chỉ cần đã authenticate (JwtAuthenticationFilter
        // đã chặn từ trước nếu chưa có token hợp lệ), không kiểm tra permission cụ thể.
        if (required == null)
            return true;
       UserDetailsImpl user = SecurityUtil.getUserDetails();
        String uri = request.getServletPath();
        if (user == null || !user.hasPermission(required.value())) {
            log.error("Inaccessible Api, Access url is {}, required permission is {}", uri, required.value());
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
