package com.erainfotech.ums.client.keycloak;

public record IdentityProviderCredentialRepresentation(
        String type,
        String value,
        boolean temporary
) {
}
