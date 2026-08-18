package com.api.user_permission.dto;

import com.api.common.constant.PermissionEffect;

/** Projection tránh phải fetch-join entity không có @ManyToOne association. */
public interface OverrideView {
    String getPermissionCode();

    PermissionEffect getEffect();
}