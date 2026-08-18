package com.api.user_permission.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.user_permission.dto.OverrideView;
import com.api.user_permission.entity.UserPermission;

import java.time.LocalDateTime;
import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermission.Id> {

    @Query("""
            SELECT p.code AS permissionCode, up.effect AS effect
            FROM UserPermission up
            JOIN Permission p ON p.id = up.id.permissionId
            WHERE up.id.userId = :userId
              AND (up.expiresAt IS NULL OR up.expiresAt > :now)
            """)
    List<OverrideView> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}