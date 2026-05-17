package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.RealmRoleResponse;
import com.erainfotech.ums.dto.UpdateUserRolesRequest;
import com.erainfotech.ums.dto.UserResponse;
import com.erainfotech.ums.service.KeycloakAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class UserManagementController {

    private final KeycloakAdminService keycloakAdminService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return keycloakAdminService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public List<UserResponse> listUsers(@RequestParam(required = false) String search) {
        return keycloakAdminService.listUsers(search);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public List<RealmRoleResponse> listRealmRoles() {
        return keycloakAdminService.listRealmRoles();
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignRoles(@PathVariable String userId,
                                    @Valid @RequestBody UpdateUserRolesRequest request) {
        return keycloakAdminService.assignRealmRoles(userId, request);
    }
}
