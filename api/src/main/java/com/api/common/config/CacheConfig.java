package com.api.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userEffectivePermissions");
        manager.setCaffeine(Caffeine.newBuilder()
                // TTL ngắn làm lưới an toàn: dù quên gọi evictCache() ở đâu đó,
                // permission cũ tối đa "sống sót" 5 phút sau khi bị thu hồi.
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(50_000));
        return manager;
    }
}