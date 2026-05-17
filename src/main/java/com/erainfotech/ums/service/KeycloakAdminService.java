package com.erainfotech.ums.service;

import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.RealmRoleResponse;
import com.erainfotech.ums.dto.UpdateUserRolesRequest;
import com.erainfotech.ums.dto.UserResponse;
import java.util.List;

public interface KeycloakAdminService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> listUsers(String search);

    UserResponse assignRealmRoles(String userId, UpdateUserRolesRequest request);

    List<RealmRoleResponse> listRealmRoles();
}
