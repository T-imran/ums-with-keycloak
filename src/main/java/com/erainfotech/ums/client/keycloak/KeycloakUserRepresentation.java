package com.erainfotech.ums.client.keycloak;

public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled
) {
}
