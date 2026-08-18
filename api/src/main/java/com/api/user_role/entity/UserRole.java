package com.api.user_role.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private Id id;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private Long grantedBy;

    /** NULL = vĩnh viễn. Role hết hạn không được tính khi resolve permission. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected UserRole() {
    }

    public UserRole(Long userId, Long roleId, Long grantedBy, LocalDateTime expiresAt) {
        this.id = new Id(userId, roleId);
        this.grantedBy = grantedBy;
        this.expiresAt = expiresAt;
    }

    public Id getId() {
        return id;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "role_id")
        private Long roleId;

        protected Id() {
        }

        public Id(Long userId, Long roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Id))
                return false;
            Id id = (Id) o;
            return Objects.equals(userId, id.userId) && Objects.equals(roleId, id.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}