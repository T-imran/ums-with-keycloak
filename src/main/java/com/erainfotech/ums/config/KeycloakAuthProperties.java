package com.erainfotech.ums.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak.auth")
public record KeycloakAuthProperties(
        @NotBlank String realm,
        @NotBlank String clientId,
        @NotBlank String clientSecret
) {
}
