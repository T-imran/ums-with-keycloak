package com.erainfotech.ums.client.keycloak;

import com.erainfotech.ums.config.KeycloakAdminProperties;
import com.erainfotech.ums.exception.IntegrationException;
import java.net.URI;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KeycloakAdminClientService {

    private final RestClient keycloakRestClient;
    private final KeycloakAdminProperties properties;
    private final KeycloakAdminTokenService keycloakAdminTokenService;

    public String realm() {
        return properties.realm();
    }

    public <T> T get(String path, Class<T> bodyType, Object... uriVariables) {
        return execute(path, () -> keycloakRestClient.get()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(bodyType));
    }

    public <T> T get(String path, ParameterizedTypeReference<T> bodyType, Object... uriVariables) {
        return execute(path, () -> keycloakRestClient.get()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(bodyType));
    }

    public URI postForLocation(String path, Object body, Object... uriVariables) {
        return execute(path, () -> keycloakRestClient.post()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation());
    }

    public void post(String path, Object body, Object... uriVariables) {
        execute(path, () -> keycloakRestClient.post()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    public void put(String path, Object body, Object... uriVariables) {
        execute(path, () -> keycloakRestClient.put()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    public void delete(String path, Object... uriVariables) {
        execute(path, () -> keycloakRestClient.delete()
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .toBodilessEntity());
    }

    public void deleteWithBody(String path, Object body, Object... uriVariables) {
        execute(path, () -> keycloakRestClient.method(HttpMethod.DELETE)
                .uri(path, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T execute(String path, Supplier<T> action) {
        try {
            return action.get();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                keycloakAdminTokenService.evictToken();
                try {
                    return action.get();
                } catch (HttpStatusCodeException retryException) {
                    throw integrationException(path, retryException);
                }
            }
            throw integrationException(path, exception);
        }
    }

    private String bearerToken() {
        return "Bearer " + keycloakAdminTokenService.getAccessToken();
    }

    private IntegrationException integrationException(String path, HttpStatusCodeException exception) {
        return new IntegrationException(
                "Keycloak admin API call failed for " + exception.getStatusCode() + " " + path + ": "
                        + exception.getResponseBodyAsString(),
                exception);
    }
}
