package com.erainfotech.ums.auth.service;

import com.erainfotech.ums.auth.dto.LoginRequest;
import com.erainfotech.ums.auth.dto.LogoutRequest;
import com.erainfotech.ums.auth.dto.RefreshTokenRequest;
import com.erainfotech.ums.auth.dto.TokenResponse;
import com.erainfotech.ums.client.keycloak.KeycloakAuthenticationClient;
import com.erainfotech.ums.config.KeycloakAuthProperties;
import com.erainfotech.ums.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final KeycloakAuthenticationClient keycloakAuthenticationClient;
    private final KeycloakAuthProperties keycloakAuthProperties;

    public TokenResponse login(LoginRequest request) {
        String clientId = resolveClientId(request.clientId());
        return keycloakAuthenticationClient.login(clientId, request.username(), request.password());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String clientId = resolveClientId(request.clientId());
        return keycloakAuthenticationClient.refresh(clientId, request.refreshToken());
    }

    public void logout(LogoutRequest request) {
        String clientId = resolveClientId(request.clientId());
        keycloakAuthenticationClient.logout(clientId, request.refreshToken());
    }

    private String resolveClientId(String requestedClientId) {
        try {
            return keycloakAuthProperties.resolveClient(requestedClientId).clientId();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(exception.getMessage());
        }
    }
}
