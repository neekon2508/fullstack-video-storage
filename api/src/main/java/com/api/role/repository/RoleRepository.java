package com.api.role.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.role.entity.Role;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // Số lượng role thường nhỏ (vài chục), load hết 1 lần để dựng cây trong RAM
    // rẻ hơn nhiều so với query đệ quy từng cấp cho mỗi request.
    List<Role> findAll();
}