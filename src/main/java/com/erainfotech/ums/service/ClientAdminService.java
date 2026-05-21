package com.erainfotech.ums.service;

import com.erainfotech.ums.client.keycloak.KeycloakAdminClientService;
import com.erainfotech.ums.client.keycloak.KeycloakClientRepresentation;
import com.erainfotech.ums.dto.ClientResponse;
import com.erainfotech.ums.dto.CreateClientRequest;
import com.erainfotech.ums.dto.UpdateClientRequest;
import com.erainfotech.ums.exception.IntegrationException;
import com.erainfotech.ums.exception.ResourceNotFoundException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class ClientAdminService {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile(".*/clients/([^/]+)$");

    private final KeycloakAdminClientService keycloakAdminClientService;

    public ClientAdminService(KeycloakAdminClientService keycloakAdminClientService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    public ClientResponse createClient(CreateClientRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", request.clientId());
        body.put("name", request.name());
        body.put("description", request.description());
        body.put("enabled", request.enabled());
        body.put("publicClient", request.publicClient());
        body.put("standardFlowEnabled", request.standardFlowEnabled());
        body.put("serviceAccountsEnabled", request.serviceAccountsEnabled());
        body.put("redirectUris", request.redirectUris() == null ? List.of() : request.redirectUris());
        body.put("webOrigins", request.webOrigins() == null ? List.of() : request.webOrigins());
        body.put("secret", request.clientSecret());

        URI location = keycloakAdminClientService.postForLocation(
                "/admin/realms/{realm}/clients",
                body,
                keycloakAdminClientService.realm());

        String internalId = extractClientId(location);
        return toResponse(getClientByInternalId(internalId));
    }

    public List<ClientResponse> listClients(String clientId) {
        String effectiveClientId = clientId == null ? "" : clientId;
        List<KeycloakClientRepresentation> clients = keycloakAdminClientService.get(
                "/admin/realms/{realm}/clients?clientId={clientId}",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm(),
                effectiveClientId);

        if (clients == null) {
            return Collections.emptyList();
        }

        return clients.stream().map(this::toResponse).toList();
    }

    public ClientResponse updateClient(String clientId, UpdateClientRequest request) {
        KeycloakClientRepresentation existing = findByClientId(clientId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", existing.id());
        body.put("clientId", existing.clientId());
        body.put("name", request.name());
        body.put("description", request.description());
        body.put("enabled", request.enabled());
        body.put("publicClient", request.publicClient());
        body.put("standardFlowEnabled", request.standardFlowEnabled());
        body.put("serviceAccountsEnabled", request.serviceAccountsEnabled());
        body.put("redirectUris", request.redirectUris() == null ? List.of() : request.redirectUris());
        body.put("webOrigins", request.webOrigins() == null ? List.of() : request.webOrigins());
        body.put("secret", request.clientSecret());
        keycloakAdminClientService.put(
                "/admin/realms/{realm}/clients/{internalId}",
                body,
                keycloakAdminClientService.realm(),
                existing.id());
        return findClient(clientId);
    }

    public ClientResponse findClient(String clientId) {
        return toResponse(findByClientId(clientId));
    }

    private KeycloakClientRepresentation getClientByInternalId(String internalId) {
        KeycloakClientRepresentation client = keycloakAdminClientService.get(
                "/admin/realms/{realm}/clients/{internalId}",
                KeycloakClientRepresentation.class,
                keycloakAdminClientService.realm(),
                internalId);

        if (client == null) {
            throw new ResourceNotFoundException("Keycloak client not found: " + internalId);
        }
        return client;
    }

    private KeycloakClientRepresentation findByClientId(String clientId) {
        List<KeycloakClientRepresentation> clients = keycloakAdminClientService.get(
                "/admin/realms/{realm}/clients?clientId={clientId}",
                new ParameterizedTypeReference<>() {
                },
                keycloakAdminClientService.realm(),
                clientId);

        if (clients == null || clients.isEmpty()) {
            throw new ResourceNotFoundException("Keycloak client not found: " + clientId);
        }

        return clients.stream()
                .filter(client -> clientId.equals(client.clientId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Keycloak client not found: " + clientId));
    }

    private ClientResponse toResponse(KeycloakClientRepresentation client) {
        return new ClientResponse(
                client.id(),
                client.clientId(),
                client.name(),
                client.description(),
                Boolean.TRUE.equals(client.enabled()),
                Boolean.TRUE.equals(client.publicClient()),
                Boolean.TRUE.equals(client.standardFlowEnabled()),
                Boolean.TRUE.equals(client.serviceAccountsEnabled()),
                client.redirectUris() == null ? List.of() : client.redirectUris(),
                client.webOrigins() == null ? List.of() : client.webOrigins());
    }

    private String extractClientId(URI location) {
        if (location == null) {
            throw new IntegrationException("Keycloak did not return a location header for the created client.");
        }

        Matcher matcher = CLIENT_ID_PATTERN.matcher(location.toString());
        if (!matcher.matches()) {
            throw new IntegrationException("Unable to extract Keycloak client id from location: " + location);
        }

        return matcher.group(1);
    }
}
