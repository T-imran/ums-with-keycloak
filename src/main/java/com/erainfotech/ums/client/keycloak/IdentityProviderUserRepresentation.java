package com.erainfotech.ums.client.keycloak;

import java.util.List;
import java.util.Map;

public record IdentityProviderUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Map<String, List<String>> attributes
) {
}
