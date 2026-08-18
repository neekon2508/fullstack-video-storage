package com.api.log;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {
    private static final String REQUEST_ID_ATTR = "request_id";
    private static final String EXCLUDED_URI_KEYWORD = "medias";

    private final ObjectMapper objectMapper;

    public void logRequest(HttpServletRequest request, Object body) {
        logHttpData("REQUEST", request, body);
    }

    public void logResponse(HttpServletRequest request, HttpServletResponse response, Object body) {
        logHttpData("RESPONSE", request, body);
    }

    private void logHttpData(String type, HttpServletRequest request, Object body) {
        // 1. Kiểm tra null-safe, log level và URI cần bỏ qua
        if (request == null || !log.isInfoEnabled()) {
            return;
        }

        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.contains(EXCLUDED_URI_KEYWORD)) {
            return;
        }
        try {
            Object requestId = request.getAttribute(REQUEST_ID_ATTR);
            String bodyStr = serializeBody(body);

            // 2. Format chuỗi log (Dùng Text Block nếu dự án chạy Java 15+)
            String logMessage = String.format(
                    "%nLOGGING %s-----------------------------------%n" +
                            "[REQUEST-ID]: %s%n" +
                            "[BODY %s]:%n%n%s%n%n" +
                            "LOGGING %s-----------------------------------",
                    type, requestId, type, bodyStr, type);

            log.info(logMessage);
        } catch (Exception e) {
            log.error("Lỗi khi serialize hoặc log {} body cho URI: {}", type, request.getRequestURI(), e);
        }
    }

    private String serializeBody(Object body) {
        if (body == null)
            return "";
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.warn("Không thể chuyển đổi BODY sang JSON: {}", e.getMessage());
            return body.toString();
        }
    }

}
