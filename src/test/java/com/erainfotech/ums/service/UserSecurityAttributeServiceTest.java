package com.erainfotech.ums.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erainfotech.ums.client.keycloak.KeycloakUserClient;
import com.erainfotech.ums.client.keycloak.ManagedUser;
import com.erainfotech.ums.model.GeoLocation;
import com.erainfotech.ums.model.LoginContext;
import com.erainfotech.ums.model.LoginStatus;
import com.erainfotech.ums.model.RiskLevel;
import com.erainfotech.ums.repository.UserLoginAuditRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSecurityAttributeServiceTest {

    @Mock
    private KeycloakUserClient keycloakUserClient;

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private UserLoginAuditRepository userLoginAuditRepository;

    private UserSecurityAttributeService userSecurityAttributeService;

    @BeforeEach
    void setUp() {
        userSecurityAttributeService = new UserSecurityAttributeService(
                keycloakUserClient,
                geoLocationService,
                userLoginAuditRepository);
    }

    @Test
    void updateLoginSecurityAttributesOnlyWritesChangedAttributes() {
        when(keycloakUserClient.getUser("user-1")).thenReturn(ManagedUser.builder()
                .id("user-1")
                .username("alice")
                .attributes(Map.of(
                        "lastLoginCountry", List.of("Bangladesh"),
                        "lastLoginDevice", List.of("device-1"),
                        "failedLoginCount", List.of("2"),
                        "accountStatus", List.of("ACTIVE")))
                .build());
        when(geoLocationService.getLocation("10.10.10.10")).thenReturn(GeoLocation.builder().build());

        LoginContext context = LoginContext.builder()
                .ipAddress("10.10.10.10")
                .country("Bangladesh")
                .city("Dhaka")
                .latitude(23.8103)
                .longitude(90.4125)
                .deviceFingerprint("device-1")
                .loginTime(OffsetDateTime.parse("2026-06-02T10:15:30Z"))
                .riskLevel(RiskLevel.MEDIUM)
                .status(LoginStatus.SUCCESS)
                .build();

        userSecurityAttributeService.updateLoginSecurityAttributes("user-1", context);

        verify(keycloakUserClient).updateUserAttributes(eq("user-1"), eq(Map.of(
                "lastLoginAt", List.of("2026-06-02T10:15:30Z"),
                "lastLoginIp", List.of("10.10.10.10"),
                "lastLoginCity", List.of("Dhaka"),
                "lastLoginLatitude", List.of("23.8103"),
                "lastLoginLongitude", List.of("90.4125"),
                "lastLoginRiskLevel", List.of("MEDIUM"))));
        verify(userLoginAuditRepository).save(any());
    }

    @Test
    void updateLoginSecurityAttributesIncrementsFailedLoginCountAndLocksAfterThreshold() {
        when(keycloakUserClient.getUser("user-2")).thenReturn(ManagedUser.builder()
                .id("user-2")
                .username("bob")
                .attributes(Map.of(
                        "failedLoginCount", List.of("4"),
                        "accountStatus", List.of("ACTIVE")))
                .build());
        when(geoLocationService.getLocation("10.10.10.20")).thenReturn(GeoLocation.builder()
                .country("Bangladesh")
                .city("Dhaka")
                .latitude(23.8103)
                .longitude(90.4125)
                .build());

        LoginContext context = LoginContext.builder()
                .ipAddress("10.10.10.20")
                .deviceFingerprint("device-2")
                .loginTime(OffsetDateTime.parse("2026-06-02T11:15:30Z"))
                .riskLevel(RiskLevel.HIGH)
                .status(LoginStatus.FAILED)
                .build();

        userSecurityAttributeService.updateLoginSecurityAttributes("user-2", context);

        verify(keycloakUserClient).updateUserAttributes(eq("user-2"), eq(Map.of(
                "lastLoginAt", List.of("2026-06-02T11:15:30Z"),
                "lastLoginIp", List.of("10.10.10.20"),
                "lastLoginCountry", List.of("Bangladesh"),
                "lastLoginCity", List.of("Dhaka"),
                "lastLoginLatitude", List.of("23.8103"),
                "lastLoginLongitude", List.of("90.4125"),
                "lastLoginDevice", List.of("device-2"),
                "lastLoginRiskLevel", List.of("HIGH"),
                "failedLoginCount", List.of("5"),
                "accountStatus", List.of("LOCKED"))));
        verify(userLoginAuditRepository).save(any());
    }

    @Test
    void helperMethodsDetectNewLocationAndDevice() {
        assertTrue(userSecurityAttributeService.isNewLocation("Bangladesh", "India"));
        assertFalse(userSecurityAttributeService.isNewLocation("Bangladesh", "bangladesh"));
        assertTrue(userSecurityAttributeService.isNewDevice("device-1", "device-2"));
        assertFalse(userSecurityAttributeService.isNewDevice("device-1", "device-1"));
    }
}
