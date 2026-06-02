package com.erainfotech.ums.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak.admin")
public record IdentityAdminProperties(
        @NotBlank String serverUrl,
        @NotBlank String realm,
        @NotBlank String adminRealm,
        @NotBlank String clientId,
        @NotBlank String clientSecret
) {
}
