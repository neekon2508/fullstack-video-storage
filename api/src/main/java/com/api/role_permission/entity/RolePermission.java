package com.api.role_permission.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private Id id;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private Long grantedBy;

    protected RolePermission() {
    }

    public RolePermission(Long roleId, Long permissionId, Long grantedBy) {
        this.id = new Id(roleId, permissionId);
        this.grantedBy = grantedBy;
    }

    public Id getId() {
        return id;
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "role_id")
        private Long roleId;

        @Column(name = "permission_id")
        private Long permissionId;

        protected Id() {
        }

        public Id(Long roleId, Long permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public Long getPermissionId() {
            return permissionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Id))
                return false;
            Id id = (Id) o;
            return Objects.equals(roleId, id.roleId) && Objects.equals(permissionId, id.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionId);
        }
    }
}