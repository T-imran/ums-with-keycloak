package com.erainfotech.ums.repository;

import com.erainfotech.ums.entity.UserLoginAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginAuditRepository extends JpaRepository<UserLoginAuditEntity, Long> {
}
