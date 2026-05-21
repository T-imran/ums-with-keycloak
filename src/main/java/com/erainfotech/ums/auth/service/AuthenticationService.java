package com.erainfotech.ums.auth.service;

import com.erainfotech.ums.auth.dto.LoginRequest;
import com.erainfotech.ums.auth.dto.LogoutRequest;
import com.erainfotech.ums.auth.dto.RefreshTokenRequest;
import com.erainfotech.ums.auth.dto.TokenResponse;
import com.erainfotech.ums.client.keycloak.KeycloakAuthenticationClient;
import com.erainfotech.ums.config.KeycloakAuthProperties;
import com.erainfotech.ums.exception.BusinessException;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final KeycloakAuthenticationClient keycloakAuthenticationClient;
    private final KeycloakAuthProperties keycloakAuthProperties;
    private final JwtDecoder jwtDecoder;

    public TokenResponse login(LoginRequest request) {
        String clientId = resolveClientId(request.clientId());
        TokenResponse tokenResponse = keycloakAuthenticationClient.login(
                clientId, request.username(), request.password());
        validateClientAccess(tokenResponse.accessToken(), clientId);
        return tokenResponse;
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

    @SuppressWarnings("unchecked")
    private void validateClientAccess(String accessToken, String clientId) {
        Jwt jwt = jwtDecoder.decode(accessToken);
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null) {
            throw new BusinessException("User does not have access to client: " + clientId);
        }

        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            throw new BusinessException("User does not have access to client: " + clientId);
        }

        Object roles = clientAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection) || roleCollection.isEmpty()) {
            throw new BusinessException("User does not have access to client: " + clientId);
        }
    }
}
