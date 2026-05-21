package com.erainfotech.ums.dto;

import java.util.List;

public record ClientResponse(
        String id,
        String clientId,
        String name,
        String description,
        boolean enabled,
        boolean publicClient,
        boolean standardFlowEnabled,
        boolean serviceAccountsEnabled,
        List<String> redirectUris,
        List<String> webOrigins
) {
}
