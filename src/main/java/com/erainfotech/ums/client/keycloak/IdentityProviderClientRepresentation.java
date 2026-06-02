package com.erainfotech.ums.client.keycloak;

import java.util.List;

public record IdentityProviderClientRepresentation(
        String id,
        String clientId,
        String name,
        String description,
        Boolean enabled,
        Boolean publicClient,
        Boolean standardFlowEnabled,
        Boolean serviceAccountsEnabled,
        List<String> redirectUris,
        List<String> webOrigins,
        String secret
) {
}
