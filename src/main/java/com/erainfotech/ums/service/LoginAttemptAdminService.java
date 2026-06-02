package com.erainfotech.ums.service;

import com.erainfotech.ums.client.keycloak.IdentityProviderAdminClientService;
import com.erainfotech.ums.client.keycloak.IdentityProviderEventRepresentation;
import com.erainfotech.ums.dto.LoginAttemptResponse;
import com.erainfotech.ums.exception.BusinessException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriBuilder;

@Service
public class LoginAttemptAdminService {

    private static final String EVENT_TYPE_LOGIN = "LOGIN";
    private static final String EVENT_TYPE_LOGIN_ERROR = "LOGIN_ERROR";
    private static final int DEFAULT_MAX = 100;

    private final IdentityProviderAdminClientService keycloakAdminClientService;

    public LoginAttemptAdminService(IdentityProviderAdminClientService keycloakAdminClientService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    public List<LoginAttemptResponse> listLoginAttempts(String userId,
                                                        String clientId,
                                                        OffsetDateTime from,
                                                        OffsetDateTime to,
                                                        Integer first,
                                                        Integer max) {
        validateRange(from, to);

        List<IdentityProviderEventRepresentation> events = keycloakAdminClientService.get(
                uriBuilder -> buildEventsUri(uriBuilder, userId, clientId, from, to, first, max),
                new ParameterizedTypeReference<>() {
                });

        if (events == null) {
            return Collections.emptyList();
        }

        return events.stream()
                .sorted(Comparator.comparingLong(IdentityProviderEventRepresentation::time).reversed())
                .map(this::toResponse)
                .toList();
    }

    private java.net.URI buildEventsUri(UriBuilder uriBuilder,
                                        String userId,
                                        String clientId,
                                        OffsetDateTime from,
                                        OffsetDateTime to,
                                        Integer first,
                                        Integer max) {
        UriBuilder builder = uriBuilder
                .path("/admin/realms/{realm}/events")
                .queryParam("type", EVENT_TYPE_LOGIN)
                .queryParam("type", EVENT_TYPE_LOGIN_ERROR)
                .queryParam("first", normalizeFirst(first))
                .queryParam("max", normalizeMax(max));

        if (userId != null && !userId.isBlank()) {
            builder = builder.queryParam("user", userId);
        }
        if (clientId != null && !clientId.isBlank()) {
            builder = builder.queryParam("client", clientId);
        }
        if (from != null) {
            builder = builder.queryParam("dateFrom", from.toInstant().toString());
        }
        if (to != null) {
            builder = builder.queryParam("dateTo", to.toInstant().toString());
        }

        return builder.build(keycloakAdminClientService.realm());
    }

    private LoginAttemptResponse toResponse(IdentityProviderEventRepresentation event) {
        Map<String, String> details = event.details() == null ? Map.of() : Map.copyOf(event.details());
        return new LoginAttemptResponse(
                event.id(),
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.time()), ZoneOffset.UTC),
                EVENT_TYPE_LOGIN_ERROR.equals(event.type()) ? "FAILURE" : "SUCCESS",
                event.type(),
                event.realmId(),
                event.clientId(),
                event.userId(),
                resolveUsername(details),
                event.ipAddress(),
                event.error(),
                details);
    }

    private String resolveUsername(Map<String, String> details) {
        String username = details.get("username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return details.getOrDefault("auth_username", null);
    }

    private int normalizeFirst(Integer first) {
        if (first == null) {
            return 0;
        }
        if (first < 0) {
            throw new BusinessException("first must be greater than or equal to 0");
        }
        return first;
    }

    private int normalizeMax(Integer max) {
        if (max == null) {
            return DEFAULT_MAX;
        }
        if (max < 1) {
            throw new BusinessException("max must be greater than 0");
        }
        return max;
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("from must be earlier than or equal to to");
        }
    }
}
