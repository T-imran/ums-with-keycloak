package com.erainfotech.ums.service.impl;

import com.erainfotech.ums.config.KeycloakAdminProperties;
import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.RealmRoleResponse;
import com.erainfotech.ums.dto.UpdateUserRolesRequest;
import com.erainfotech.ums.dto.UserResponse;
import com.erainfotech.ums.exception.IntegrationException;
import com.erainfotech.ums.service.KeycloakAdminService;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile(".*/users/([^/]+)$");

    private final RestClient keycloakRestClient;
    private final KeycloakAdminProperties properties;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String token = obtainAdminAccessToken();
        URI location;

        try {
            location = keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/users", properties.managedRealm())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of(
                            "username", request.username(),
                            "email", request.email(),
                            "firstName", request.firstName(),
                            "lastName", request.lastName(),
                            "enabled", request.enabled(),
                            "emailVerified", Boolean.FALSE,
                            "credentials", List.of(Map.of(
                                    "type", "password",
                                    "value", request.password(),
                                    "temporary", request.temporaryPassword()))))
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException("Unable to create Keycloak user: " + exception.getResponseBodyAsString(), exception);
        }

        String userId = extractUserId(location);
        assignRolesInternal(userId, request.roles(), token);
        return findUserById(userId, token, request.roles());
    }

    @Override
    public List<UserResponse> listUsers(String search) {
        String token = obtainAdminAccessToken();

        try {
            List<KeycloakUserRepresentation> users = keycloakRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParamIfPresent("search", java.util.Optional.ofNullable(search))
                            .build(properties.managedRealm()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (users == null) {
                return Collections.emptyList();
            }

            return users.stream()
                    .map(user -> new UserResponse(
                            user.id(),
                            user.username(),
                            user.email(),
                            user.firstName(),
                            user.lastName(),
                            Boolean.TRUE.equals(user.enabled()),
                            Collections.emptySet()))
                    .toList();
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException("Unable to fetch users from Keycloak: " + exception.getResponseBodyAsString(), exception);
        }
    }

    @Override
    public UserResponse assignRealmRoles(String userId, UpdateUserRolesRequest request) {
        String token = obtainAdminAccessToken();
        assignRolesInternal(userId, request.roles(), token);
        return findUserById(userId, token, request.roles());
    }

    @Override
    public List<RealmRoleResponse> listRealmRoles() {
        String token = obtainAdminAccessToken();
        List<KeycloakRoleRepresentation> roles = getRealmRoles(token);
        return roles.stream()
                .map(role -> new RealmRoleResponse(role.id(), normalizeRole(role.name()), role.description()))
                .toList();
    }

    private void assignRolesInternal(String userId, Set<String> roles, String token) {
        List<KeycloakRoleRepresentation> availableRoles = getRealmRoles(token);

        if (availableRoles == null) {
            throw new IntegrationException("Keycloak returned no roles for realm " + properties.managedRealm());
        }

        List<Map<String, Object>> matchingRoles = availableRoles.stream()
                .filter(role -> roles.contains(role.name()) || roles.contains("ROLE_" + role.name()))
                .map(role -> Map.<String, Object>of(
                        "id", role.id(),
                        "name", role.name()))
                .toList();

        if (matchingRoles.size() != roles.size()) {
            throw new IntegrationException("One or more requested Keycloak roles do not exist in realm " + properties.managedRealm());
        }

        keycloakRestClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.managedRealm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(matchingRoles)
                .retrieve()
                .toBodilessEntity();
    }

    private List<KeycloakRoleRepresentation> getRealmRoles(String token) {
        return keycloakRestClient.get()
                .uri("/admin/realms/{realm}/roles", properties.managedRealm())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private UserResponse findUserById(String userId, String token, Set<String> assignedRoles) {
        KeycloakUserRepresentation user = keycloakRestClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", properties.managedRealm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KeycloakUserRepresentation.class);

        if (user == null) {
            throw new IntegrationException("Unable to read created Keycloak user.");
        }

        return new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName(),
                Boolean.TRUE.equals(user.enabled()),
                assignedRoles.stream().map(this::normalizeRole).collect(java.util.stream.Collectors.toSet()));
    }

    private String obtainAdminAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());

        try {
            TokenResponse tokenResponse = keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.adminRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
                throw new IntegrationException("Keycloak admin token response was empty.");
            }
            return tokenResponse.accessToken();
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException("Unable to obtain Keycloak admin access token: " + exception.getResponseBodyAsString(), exception);
        }
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

    private String normalizeRole(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    private record TokenResponse(String access_token) {
        String accessToken() {
            return access_token;
        }
    }

    private record KeycloakRoleRepresentation(String id, String name, String description) {
    }

    private record KeycloakUserRepresentation(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            Boolean enabled
    ) {
    }
}
