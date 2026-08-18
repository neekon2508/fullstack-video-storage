package com.api.user_role.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.user_role.entity.UserRole;

import java.time.LocalDateTime;
import java.util.Set;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Id> {

    // Chỉ lấy role chưa hết hạn: expires_at IS NULL (vĩnh viễn) hoặc còn hạn.
    @Query("""
            SELECT ur.id.roleId
            FROM UserRole ur
            WHERE ur.id.userId = :userId
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    Set<Long> findActiveRoleIdsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}