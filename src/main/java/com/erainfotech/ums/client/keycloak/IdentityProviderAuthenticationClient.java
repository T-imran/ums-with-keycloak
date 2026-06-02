package com.erainfotech.ums.client.keycloak;

import com.erainfotech.ums.auth.dto.TokenResponse;
import com.erainfotech.ums.config.IdentityAuthProperties;
import com.erainfotech.ums.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class IdentityProviderAuthenticationClient {

    private final RestClient keycloakRestClient;
    private final IdentityAuthProperties properties;

    public TokenResponse login(String clientId, String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", username);
        form.add("password", password);
        return exchange(form, "Unable to authenticate user with Keycloak.");
    }

    public TokenResponse refresh(String clientId, String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        return exchange(form, "Unable to refresh Keycloak token.");
    }

    public void logout(String clientId, String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);

        try {
            keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/logout", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException(
                    "Unable to logout from Keycloak: " + exception.getResponseBodyAsString(),
                    exception);
        }
    }

    private TokenResponse exchange(MultiValueMap<String, String> form, String message) {
        try {
            TokenResponse response = keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IntegrationException(message + " Empty token response.");
            }
            return response;
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException(message + " " + exception.getResponseBodyAsString(), exception);
        }
    }
}
