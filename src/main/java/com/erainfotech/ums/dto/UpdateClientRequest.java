package com.erainfotech.ums.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateClientRequest(
        @NotBlank String name,
        String description,
        boolean enabled,
        boolean publicClient,
        boolean standardFlowEnabled,
        boolean serviceAccountsEnabled,
        List<String> redirectUris,
        List<String> webOrigins,
        String clientSecret
) {
}
