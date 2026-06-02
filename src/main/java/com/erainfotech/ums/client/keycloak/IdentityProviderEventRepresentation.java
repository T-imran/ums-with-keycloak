package com.erainfotech.ums.client.keycloak;

import java.util.Map;

public record IdentityProviderEventRepresentation(
        String id,
        long time,
        String type,
        String realmId,
        String clientId,
        String userId,
        String sessionId,
        String ipAddress,
        String error,
        Map<String, String> details
) {
}
