package com.erainfotech.ums.service;

import com.erainfotech.ums.client.keycloak.IdentityProviderAdminClientService;
import com.erainfotech.ums.client.keycloak.IdentityProviderRoleRepresentation;
import com.erainfotech.ums.dto.CreateRealmRoleRequest;
import com.erainfotech.ums.dto.RealmRoleResponse;
import com.erainfotech.ums.dto.UpdateUserRolesRequest;
import com.erainfotech.ums.exception.IntegrationException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class RoleAdminService {

    private final IdentityProviderAdminClientService keycloakAdminClientService;

    public RoleAdminService(IdentityProviderAdminClientService keycloakAdminClientService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    public RealmRoleResponse createRealmRole(CreateRealmRoleRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.name());
        body.put("description", request.description());
        keycloakAdminClientService.post(
                "/admin/realms/{realm}/roles",
                body,
                keycloakAdminClientService.realm());

        IdentityProviderRoleRepresentation role = keycloakAdminClientService.get(
                "/admin/realms/{realm}/roles/{roleName}",
                IdentityProviderRoleRepresentation.class,
                keycloakAdminClientService.realm(),
                request.name());
        return toResponse(role);
    }

    public List<RealmRoleResponse> listRealmRoles() {
        List<IdentityProviderRoleRepresentation> roles = keycloakAdminClientService.get(
                "/admin/realms/{realm}/roles",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm());

        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream().map(this::toResponse).toList();
    }

    public void assignRealmRoles(String userId, UpdateUserRolesRequest request) {
        List<Map<String, Object>> roles = resolveRoleMappings(request.roles());
        keycloakAdminClientService.post(
                "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                roles,
                keycloakAdminClientService.realm(),
                userId);
    }

    public void removeRealmRoles(String userId, UpdateUserRolesRequest request) {
        List<Map<String, Object>> roles = resolveRoleMappings(request.roles());
        keycloakAdminClientService.deleteWithBody(
                "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                roles,
                keycloakAdminClientService.realm(),
                userId);
    }

    public Set<String> getUserRealmRoles(String userId) {
        List<IdentityProviderRoleRepresentation> roles = keycloakAdminClientService.get(
                "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm(),
                userId);

        if (roles == null) {
            return Collections.emptySet();
        }

        return roles.stream().map(IdentityProviderRoleRepresentation::name).map(this::normalizeRole).collect(java.util.stream.Collectors.toSet());
    }

    private List<Map<String, Object>> resolveRoleMappings(Set<String> requestedRoles) {
        List<IdentityProviderRoleRepresentation> realmRoles = keycloakAdminClientService.get(
                "/admin/realms/{realm}/roles",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm());

        if (realmRoles == null) {
            throw new IntegrationException("Keycloak returned no realm roles for " + keycloakAdminClientService.realm());
        }

        List<Map<String, Object>> matchingRoles = realmRoles.stream()
                .filter(role -> requestedRoles.stream().anyMatch(requested -> normalizeRole(requested).equals(normalizeRole(role.name()))))
                .map(role -> Map.<String, Object>of("id", role.id(), "name", role.name()))
                .toList();

        if (matchingRoles.size() != requestedRoles.size()) {
            throw new IntegrationException("One or more requested realm roles do not exist in Keycloak.");
        }

        return matchingRoles;
    }

    private RealmRoleResponse toResponse(IdentityProviderRoleRepresentation role) {
        return new RealmRoleResponse(role.id(), normalizeRole(role.name()), role.description());
    }

    private String normalizeRole(String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
