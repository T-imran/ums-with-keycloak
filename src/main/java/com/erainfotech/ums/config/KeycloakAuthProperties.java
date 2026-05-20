package com.erainfotech.ums.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak.auth")
public record KeycloakAuthProperties(
        @NotBlank String serverUrl,
        @NotBlank String realm,
        @NotEmpty Map<String, PublicClientProperties> clients
) {

    public PublicClientProperties resolveClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required.");
        }

        PublicClientProperties client = clients.get(clientId);
        if (client == null || client.clientId() == null || client.clientId().isBlank()) {
            throw new IllegalArgumentException("Unsupported clientId: " + clientId);
        }

        return client;
    }

    public record PublicClientProperties(
            @NotBlank String clientId
    ) {
    }
}
