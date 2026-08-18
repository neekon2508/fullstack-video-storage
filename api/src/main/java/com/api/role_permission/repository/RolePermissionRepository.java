package com.api.role_permission.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.role_permission.entity.RolePermission;

import java.util.Collection;
import java.util.Set;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Id> {

    // Join permissions để trả thẳng permission.code, tránh phải query permissions
    // riêng.
    @Query("""
            SELECT p.code
            FROM RolePermission rp
            JOIN Permission p ON p.id = rp.id.permissionId
            WHERE rp.id.roleId IN :roleIds
            """)
    Set<String> findPermissionCodesByRoleIds(@Param("roleIds") Collection<Long> roleIds);
}
