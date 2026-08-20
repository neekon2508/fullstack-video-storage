package com.api.autho.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.api.common.constant.PermissionEffect;
import com.api.role.entity.Role;
import com.api.role.repository.RoleRepository;
import com.api.role_permission.repository.RolePermissionRepository;
import com.api.user_permission.dto.OverrideView;
import com.api.user_permission.entity.UserPermission;
import com.api.user_permission.repository.UserPermissionRepository;
import com.api.user_role.entity.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolve tập permission hiệu lực của 1 user:
 * 1. user_roles (chưa expire) -> mở rộng qua parent_role_id (kế thừa hierarchy)
 * -> role_permissions.
 * 2. user_permissions ALLOW override (chưa expire) -> cộng thêm.
 * 3. user_permissions DENY override (chưa expire) -> trừ đi CUỐI CÙNG,
 * nên DENY luôn thắng ALLOW dù ALLOW đến từ role hay từ override.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final RoleRepository roleRepository;

    public boolean hasPermission(Long userId, String permissionCode) {
        return resolveEffectivePermissions(userId).contains(permissionCode);
    }

    @Cacheable(cacheNames = "userEffectivePermissions", key = "#id")
    public Set<String> resolveEffectivePermissions(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        Set<Long> directRoleIds = userRoleRepository.findActiveRoleIdsByUserId(userId, now);
        Set<Long> allRoleIds = expandRoleHierarchy(directRoleIds);

        Set<String> permissions = allRoleIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(rolePermissionRepository.findPermissionCodesByRoleIds(allRoleIds));

        List<OverrideView> overrides = userPermissionRepository.findActiveByUserId(userId,
                now);

        Set<String> allow = overrides.stream()
                .filter(o -> o.getEffect() == PermissionEffect.ALLOW)
                .map(OverrideView::getPermissionCode)
                .collect(Collectors.toSet());

        Set<String> deny = overrides.stream()
                .filter(o -> o.getEffect() == PermissionEffect.DENY)
                .map(OverrideView::getPermissionCode)
                .collect(Collectors.toSet());

        permissions.addAll(allow);
        permissions.removeAll(deny); // DENY thắng ALLOW - luôn áp cuối cùng

        return permissions;
    }

    private Set<Long> expandRoleHierarchy(Set<Long> directRoleIds) {
        if (directRoleIds.isEmpty())
            return Set.of();

        Map<Long, Long> parentOf = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getId, r -> r.getParentRoleId() == null ? -1L : r.getParentRoleId()));

        Set<Long> result = new HashSet<>();
        for (Long roleId : directRoleIds) {
            Set<Long> visited = new HashSet<>();
            Long current = roleId;
            while (current != null && current != -1L && visited.add(current)) {
                result.add(current);
                current = parentOf.get(current);
            }
        }
        return result;
    }

    /** Gọi ngay sau khi admin gán/thu hồi role hoặc permission cho user. */
    @CacheEvict(cacheNames = "userEffectivePermissions", key = "#id")
    public void evictCache(Long userId) {
        // no-op
    }
}