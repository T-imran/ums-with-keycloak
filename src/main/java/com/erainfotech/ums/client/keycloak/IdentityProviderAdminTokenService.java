package com.erainfotech.ums.client.keycloak;

import com.erainfotech.ums.config.IdentityAdminProperties;
import com.erainfotech.ums.exception.IntegrationException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class IdentityProviderAdminTokenService {

    private static final long DEFAULT_EXPIRY_SKEW_SECONDS = 30L;

    private final RestClient keycloakRestClient;
    private final IdentityAdminProperties properties;
    private final Clock clock = Clock.systemUTC();

    private final Object monitor = new Object();
    private volatile CachedAccessToken cachedAccessToken;

    public String getAccessToken() {
        CachedAccessToken currentToken = cachedAccessToken;
        if (currentToken != null && !currentToken.isExpired(clock)) {
            return currentToken.accessToken();
        }

        synchronized (monitor) {
            currentToken = cachedAccessToken;
            if (currentToken != null && !currentToken.isExpired(clock)) {
                return currentToken.accessToken();
            }

            CachedAccessToken refreshedToken = requestNewToken();
            cachedAccessToken = refreshedToken;
            return refreshedToken.accessToken();
        }
    }

    public void evictToken() {
        synchronized (monitor) {
            cachedAccessToken = null;
        }
    }

    private CachedAccessToken requestNewToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        try {
            IdentityProviderTokenResponse response = keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.adminRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(IdentityProviderTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IntegrationException("Keycloak admin token response was empty.");
            }

            long expiresIn = response.expiresIn() > 0 ? response.expiresIn() : DEFAULT_EXPIRY_SKEW_SECONDS;
            Instant expiresAt = Instant.now(clock).plusSeconds(Math.max(1L, expiresIn - DEFAULT_EXPIRY_SKEW_SECONDS));
            return new CachedAccessToken(response.accessToken(), expiresAt);
        } catch (HttpStatusCodeException exception) {
            throw new IntegrationException(
                    "Unable to obtain Keycloak admin access token: " + exception.getResponseBodyAsString(),
                    exception);
        }
    }

    private record CachedAccessToken(String accessToken, Instant expiresAt) {
        boolean isExpired(Clock clock) {
            return Instant.now(clock).isAfter(expiresAt);
        }
    }
}
