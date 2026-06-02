package com.erainfotech.ums.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.erainfotech.ums.client.keycloak.IdentityProviderAdminClientService;
import com.erainfotech.ums.client.keycloak.IdentityProviderEventRepresentation;
import com.erainfotech.ums.dto.LoginAttemptResponse;
import com.erainfotech.ums.exception.BusinessException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

@ExtendWith(MockitoExtension.class)
class LoginAttemptAdminServiceTest {

    @Mock
    private IdentityProviderAdminClientService keycloakAdminClientService;

    private LoginAttemptAdminService loginAttemptAdminService;

    @BeforeEach
    void setUp() {
        loginAttemptAdminService = new LoginAttemptAdminService(keycloakAdminClientService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listLoginAttemptsMapsSuccessAndFailureEvents() {
        when(keycloakAdminClientService.get(
                any(Function.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(
                        new IdentityProviderEventRepresentation(
                                "event-1",
                                1_717_281_600_000L,
                                "LOGIN",
                                "bank-asia",
                                "ums-app",
                                "user-1",
                                "session-1",
                                "10.0.0.1",
                                null,
                                Map.of("username", "alice")),
                        new IdentityProviderEventRepresentation(
                                "event-2",
                                1_717_281_700_000L,
                                "LOGIN_ERROR",
                                "bank-asia",
                                "ums-app",
                                null,
                                null,
                                "10.0.0.2",
                                "invalid_user_credentials",
                                Map.of("username", "bob"))));

        List<LoginAttemptResponse> attempts = loginAttemptAdminService.listLoginAttempts(
                null, null, null, null, null, null);

        assertEquals(2, attempts.size());
        assertEquals("event-2", attempts.get(0).eventId());
        assertEquals("FAILURE", attempts.get(0).status());
        assertEquals("bob", attempts.get(0).username());
        assertEquals("invalid_user_credentials", attempts.get(0).error());
        assertEquals(OffsetDateTime.of(2024, 6, 1, 22, 41, 40, 0, ZoneOffset.UTC), attempts.get(0).occurredAt());

        assertEquals("event-1", attempts.get(1).eventId());
        assertEquals("SUCCESS", attempts.get(1).status());
        assertEquals("alice", attempts.get(1).username());
    }

    @Test
    void listLoginAttemptsRejectsInvalidDateRange() {
        OffsetDateTime from = OffsetDateTime.parse("2026-06-02T10:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-06-02T09:00:00Z");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> loginAttemptAdminService.listLoginAttempts(null, null, from, to, null, null));

        assertEquals("from must be earlier than or equal to to", exception.getMessage());
    }
}
