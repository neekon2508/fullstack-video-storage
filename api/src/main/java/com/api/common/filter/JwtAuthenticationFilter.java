package com.api.common.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.api.auth.service.UserDetailsImpl;
import com.api.autho.service.AuthorizationService;
import com.api.common.constant.CommonConstants;
import com.api.common.exception.ErrorCode;
import com.api.common.model.CommonResponse;
import com.api.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getServletPath();
            if (requestURI.startsWith("/swagger-ui") ||
                    requestURI.startsWith("/v3/api-docs") ||
                    requestURI.startsWith("/login")) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token, false)) {
                    Claims claims = jwtUtil.extractClaims(token, false);
                    Long id = claims.get("id", Long.class);
                    String username = claims.get("username", String.class);
                    // --- Autho: resolve permission ngay sau khi Authen thành công ---
                    // Có cache (Caffeine, xem CacheConfig) nên không tốn query mỗi request
                    // trừ lần đầu / sau khi cache hết hạn hoặc bị evict.
                    Set<String> permissions = authorizationService.resolveEffectivePermissions(id);
                    UserDetailsImpl userDetails = UserDetailsImpl
                            .builder()
                            .id(id)
                            .username(username)
                            .permissions(permissions)
                            .build();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, Collections.emptyList());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else
                    log.warn("Token expired or invalid");
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.warn("[JwtAuthenticationFilter] Token hết hạn: {}", e.getMessage());
            handleException(response, ErrorCode.JWT_EXPIRED.getCode(), HttpStatus.UNAUTHORIZED);

        } catch (JwtException e) {
            log.warn("[JwtAuthenticationFilter] Token không hợp lệ: {}", e.getMessage());
            handleException(response, ErrorCode.INVALID_TOKEN.getCode(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("[JwtAuthenticationFilter] Lỗi xác thực: {}", e.getMessage(), e);
            handleException(response, ErrorCode.AUTH_ERROR.getCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void handleException(HttpServletResponse response, String statusCode, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonResponse<?> errorResponse = CommonResponse.builder()
                .successOrNot(CommonConstants.NO_FLAG)
                .statusCode(statusCode)
                .data("EXCEPTION")
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
