package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.CreateRealmRoleRequest;
import com.erainfotech.ums.dto.RealmRoleResponse;
import com.erainfotech.ums.service.RoleAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public List<RealmRoleResponse> listRealmRoles() {
        return roleAdminService.listRealmRoles();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public RealmRoleResponse createRealmRole(@Valid @RequestBody CreateRealmRoleRequest request) {
        return roleAdminService.createRealmRole(request);
    }
}
