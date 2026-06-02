package com.erainfotech.ums.service;

import com.erainfotech.ums.client.keycloak.IdentityProviderAdminClientService;
import com.erainfotech.ums.client.keycloak.IdentityProviderCredentialRepresentation;
import com.erainfotech.ums.client.keycloak.IdentityProviderUserRepresentation;
import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.ResetPasswordRequest;
import com.erainfotech.ums.dto.UpdateUserRequest;
import com.erainfotech.ums.dto.UserResponse;
import com.erainfotech.ums.dto.UserStatusUpdateRequest;
import com.erainfotech.ums.exception.IntegrationException;
import com.erainfotech.ums.exception.ResourceNotFoundException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile(".*/users/([^/]+)$");

    private final IdentityProviderAdminClientService keycloakAdminClientService;
    private final RoleAdminService roleAdminService;

    public UserAdminService(IdentityProviderAdminClientService keycloakAdminClientService,
                            RoleAdminService roleAdminService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.roleAdminService = roleAdminService;
    }

    public UserResponse createUser(CreateUserRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.username());
        body.put("email", request.email());
        body.put("firstName", request.firstName());
        body.put("lastName", request.lastName());
        body.put("enabled", request.enabled());
        body.put("emailVerified", Boolean.FALSE);
        body.put("credentials", List.of(new IdentityProviderCredentialRepresentation(
                "password",
                request.password(),
                request.temporaryPassword())));

        URI location = keycloakAdminClientService.postForLocation(
                "/admin/realms/{realm}/users",
                body,
                keycloakAdminClientService.realm());

        String userId = extractUserId(location);
        roleAdminService.assignRealmRoles(userId, new com.erainfotech.ums.dto.UpdateUserRolesRequest(request.roles()));
        return getUser(userId);
    }

    public List<UserResponse> listUsers(String search) {
        List<IdentityProviderUserRepresentation> users = keycloakAdminClientService.get(
                "/admin/realms/{realm}/users?search={search}",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm(),
                search == null ? "" : search);

        if (users == null) {
            return Collections.emptyList();
        }

        return users.stream().map(user -> toResponse(user, roleAdminService.getUserRealmRoles(user.id()))).toList();
    }

    public UserResponse getUser(String userId) {
        IdentityProviderUserRepresentation user = keycloakAdminClientService.get(
                "/admin/realms/{realm}/users/{userId}",
                IdentityProviderUserRepresentation.class,
                keycloakAdminClientService.realm(),
                userId);

        if (user == null) {
            throw new ResourceNotFoundException("Keycloak user not found: " + userId);
        }

        return toResponse(user, roleAdminService.getUserRealmRoles(userId));
    }

    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        IdentityProviderUserRepresentation existing = requireUser(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", existing.username());
        body.put("email", request.email());
        body.put("firstName", request.firstName());
        body.put("lastName", request.lastName());
        body.put("enabled", Boolean.TRUE.equals(existing.enabled()));
        keycloakAdminClientService.put(
                "/admin/realms/{realm}/users/{userId}",
                body,
                keycloakAdminClientService.realm(),
                userId);
        return getUser(userId);
    }

    public UserResponse updateStatus(String userId, UserStatusUpdateRequest request) {
        IdentityProviderUserRepresentation existing = requireUser(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", existing.username());
        body.put("email", existing.email());
        body.put("firstName", existing.firstName());
        body.put("lastName", existing.lastName());
        body.put("enabled", request.enabled());
        keycloakAdminClientService.put(
                "/admin/realms/{realm}/users/{userId}",
                body,
                keycloakAdminClientService.realm(),
                userId);
        return getUser(userId);
    }

    public void resetPassword(String userId, ResetPasswordRequest request) {
        requireUser(userId);
        keycloakAdminClientService.put(
                "/admin/realms/{realm}/users/{userId}/reset-password",
                new IdentityProviderCredentialRepresentation("password", request.password(), request.temporary()),
                keycloakAdminClientService.realm(),
                userId);
    }

    public void deleteUser(String userId) {
        requireUser(userId);
        keycloakAdminClientService.delete(
                "/admin/realms/{realm}/users/{userId}",
                keycloakAdminClientService.realm(),
                userId);
    }

    private IdentityProviderUserRepresentation requireUser(String userId) {
        IdentityProviderUserRepresentation user = keycloakAdminClientService.get(
                "/admin/realms/{realm}/users/{userId}",
                IdentityProviderUserRepresentation.class,
                keycloakAdminClientService.realm(),
                userId);
        if (user == null) {
            throw new ResourceNotFoundException("Keycloak user not found: " + userId);
        }
        return user;
    }

    private UserResponse toResponse(IdentityProviderUserRepresentation user, Set<String> roles) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName(),
                Boolean.TRUE.equals(user.enabled()),
                roles,
                copyAttributes(user.attributes()));
    }

    private Map<String, List<String>> copyAttributes(Map<String, List<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> copy = new LinkedHashMap<>();
        attributes.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }

    private String extractUserId(URI location) {
        if (location == null) {
            throw new IntegrationException("Keycloak did not return a location header for the created user.");
        }

        Matcher matcher = USER_ID_PATTERN.matcher(location.toString());
        if (!matcher.matches()) {
            throw new IntegrationException("Unable to extract Keycloak user id from location: " + location);
        }

        return matcher.group(1);
    }
}
