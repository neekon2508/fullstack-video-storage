package com.api.autho.service;

import java.util.Set;

public interface AuthorizationService {
    boolean hasPermission(Long userId, String permissionCode);

    Set<String> resolveEffectivePermissions(Long userId);
    
    void evictCache(Long userId);
}
