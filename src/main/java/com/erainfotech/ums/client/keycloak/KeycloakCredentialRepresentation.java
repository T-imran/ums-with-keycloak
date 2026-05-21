package com.erainfotech.ums.client.keycloak;

public record KeycloakCredentialRepresentation(
        String type,
        String value,
        boolean temporary
) {
}
