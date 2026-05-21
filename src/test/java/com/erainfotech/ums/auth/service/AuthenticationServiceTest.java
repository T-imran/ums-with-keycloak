package com.erainfotech.ums.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erainfotech.ums.auth.dto.LoginRequest;
import com.erainfotech.ums.auth.dto.TokenResponse;
import com.erainfotech.ums.client.keycloak.KeycloakAuthenticationClient;
import com.erainfotech.ums.config.KeycloakAuthProperties;
import com.erainfotech.ums.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private KeycloakAuthenticationClient keycloakAuthenticationClient;

    @Mock
    private JwtDecoder jwtDecoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        KeycloakAuthProperties properties = new KeycloakAuthProperties(
                "http://localhost:8080",
                "bank-asia",
                Map.of(
                        "ums-ui",
                        new KeycloakAuthProperties.PublicClientProperties("ums-admin-app")));
        authenticationService = new AuthenticationService(
                keycloakAuthenticationClient,
                properties,
                jwtDecoder);
    }

    @Test
    void loginAllowsUserWhenRequestedClientExistsInResourceAccess() {
        LoginRequest request = new LoginRequest("alice", "secret", "ums-ui");
        TokenResponse tokenResponse = new TokenResponse(
                "access-token", 300, 1800, "refresh-token", "Bearer", "openid profile");

        when(keycloakAuthenticationClient.login("ums-admin-app", "alice", "secret"))
                .thenReturn(tokenResponse);
        when(jwtDecoder.decode("access-token")).thenReturn(jwtWithResourceAccess(
                Map.of("ums-admin-app", Map.of("roles", List.of("ROLE_UMS_CLIENT")))));

        TokenResponse actual = authenticationService.login(request);

        assertSame(tokenResponse, actual);
        verify(keycloakAuthenticationClient).login("ums-admin-app", "alice", "secret");
        verify(jwtDecoder).decode("access-token");
    }

    @Test
    void loginRejectsUserWhenRequestedClientDoesNotExistInResourceAccess() {
        LoginRequest request = new LoginRequest("alice", "secret", "ums-ui");
        TokenResponse tokenResponse = new TokenResponse(
                "access-token", 300, 1800, "refresh-token", "Bearer", "openid profile");

        when(keycloakAuthenticationClient.login("ums-admin-app", "alice", "secret"))
                .thenReturn(tokenResponse);
        when(jwtDecoder.decode("access-token")).thenReturn(jwtWithResourceAccess(
                Map.of("another-client", Map.of("roles", List.of("ROLE_OTHER")))));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.login(request));

        assertEquals("User does not have access to client: ums-admin-app", exception.getMessage());
    }

    private Jwt jwtWithResourceAccess(Map<String, Object> resourceAccess) {
        return new Jwt(
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1", "resource_access", resourceAccess));
    }
}
