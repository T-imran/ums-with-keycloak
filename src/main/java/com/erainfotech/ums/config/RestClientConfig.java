package com.erainfotech.ums.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient keycloakRestClient(RestClient.Builder builder, IdentityAdminProperties properties) {
        return builder
                .baseUrl(properties.serverUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
