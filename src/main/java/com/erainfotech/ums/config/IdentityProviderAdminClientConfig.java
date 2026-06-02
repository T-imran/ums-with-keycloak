package com.erainfotech.ums.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class IdentityProviderAdminClientConfig {

    private final IdentityAdminProperties identityAdminProperties;

    @Bean(destroyMethod = "close")
    Keycloak identityProviderAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(identityAdminProperties.serverUrl())
                .realm(identityAdminProperties.adminRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(identityAdminProperties.clientId())
                .clientSecret(identityAdminProperties.clientSecret())
                .build();
    }
}
