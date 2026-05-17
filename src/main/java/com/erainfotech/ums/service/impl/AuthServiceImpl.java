package com.erainfotech.ums.service.impl;

import com.erainfotech.ums.config.KeycloakAuthProperties;
import com.erainfotech.ums.dto.CreateUserRequest;
import com.erainfotech.ums.dto.LoginRequest;
import com.erainfotech.ums.dto.LogoutRequest;
import com.erainfotech.ums.dto.RefreshTokenRequest;
import com.erainfotech.ums.dto.RegisterRequest;
import com.erainfotech.ums.dto.TokenResponse;
import com.erainfotech.ums.dto.UserResponse;
import com.erainfotech.ums.exception.IntegrationException;
import com.erainfotech.ums.security.SecurityConstants;
import com.erainfotech.ums.service.AuthService;
import com.erainfotech.ums.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RestClient keycloakRestClient;
    private final KeycloakAuthProperties keycloakAuthProperties;
    private final KeycloakAdminService keycloakAdminService;

    @Override
    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakAuthProperties.clientId());
        form.add("client_secret", keycloakAuthProperties.clientSecret());
        form.add("username", request.username());
        form.add("password", request.password());
        return exchangeToken(form, "Unable to complete login with Keycloak.");
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        return keycloakAdminService.createUser(new CreateUserRequest(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                false,
                java.util.Set.of(SecurityConstants.ROLE_CUSTOMER),
                true));
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakAuthProperties.clientId());
        form.add("client_secret", keycloakAuthProperties.clientSecret());
        form.add("refresh_token", request.refreshToken());
        return exchangeToken(form, "Unable to refresh Keycloak access token.");
    }

    @Override
    public void logout(LogoutRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakAuthProperties.clientId());
        form.add("client_secret", keycloakAuthProperties.clientSecret());
        form.add("refresh_token", request.refreshToken());

        try {
            keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/logout", keycloakAuthProperties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException("Unable to logout from Keycloak: " + exception.getResponseBodyAsString(), exception);
        }
    }

    private TokenResponse exchangeToken(MultiValueMap<String, String> form, String message) {
        try {
            TokenResponse response = keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keycloakAuthProperties.realm())
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
