package com.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gắn lên controller method (hoặc class) để khai báo permission code cần có
 * để gọi API này, vd:
 *
 * @RequirePermission("media:write")
 * @PutMapping("/api/media/{id}")
 * public ... updateMediaItem(...) { ... }
 *
 * API không gắn annotation này mặc định chỉ cần đã authenticate (không kiểm
 * tra permission cụ thể) - xem AuthorizationInterceptor.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
}