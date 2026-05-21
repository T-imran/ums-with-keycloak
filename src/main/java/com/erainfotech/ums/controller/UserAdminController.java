package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.ResetPasswordRequest;
import com.erainfotech.ums.dto.UpdateUserRolesRequest;
import com.erainfotech.ums.dto.UpdateUserRequest;
import com.erainfotech.ums.dto.UserResponse;
import com.erainfotech.ums.dto.UserStatusUpdateRequest;
import com.erainfotech.ums.service.RoleAdminService;
import com.erainfotech.ums.service.UserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final RoleAdminService roleAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userAdminService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public List<UserResponse> listUsers(@RequestParam(required = false) String search) {
        return userAdminService.listUsers(search);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public UserResponse getUser(@PathVariable String userId) {
        return userAdminService.getUser(userId);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public UserResponse updateUser(@PathVariable String userId,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userAdminService.updateUser(userId, request);
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public UserResponse updateStatus(@PathVariable String userId,
                                     @RequestBody UserStatusUpdateRequest request) {
        return userAdminService.updateStatus(userId, request);
    }

    @PostMapping("/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public void resetPassword(@PathVariable String userId,
                              @Valid @RequestBody ResetPasswordRequest request) {
        userAdminService.resetPassword(userId, request);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public UserResponse assignRoles(@PathVariable String userId,
                                    @Valid @RequestBody UpdateUserRolesRequest request) {
        roleAdminService.assignRealmRoles(userId, request);
        return userAdminService.getUser(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public void deleteUser(@PathVariable String userId) {
        userAdminService.deleteUser(userId);
    }
}
