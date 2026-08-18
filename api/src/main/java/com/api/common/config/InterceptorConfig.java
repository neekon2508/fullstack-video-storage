package com.api.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.api.common.interceptor.AuthorizationInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;

    @Value("${interceptor.authorization.exclude-paths}")
    private List<String> authorizationExcludePaths;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authorizationInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(authorizationExcludePaths);
    }
}
