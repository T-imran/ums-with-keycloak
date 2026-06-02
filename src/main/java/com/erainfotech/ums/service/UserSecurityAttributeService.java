package com.erainfotech.ums.service;

import com.erainfotech.ums.client.keycloak.KeycloakUserClient;
import com.erainfotech.ums.client.keycloak.ManagedUser;
import com.erainfotech.ums.entity.UserLoginAuditEntity;
import com.erainfotech.ums.model.GeoLocation;
import com.erainfotech.ums.model.LoginContext;
import com.erainfotech.ums.model.LoginStatus;
import com.erainfotech.ums.repository.UserLoginAuditRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityAttributeService {

    static final String ATTRIBUTE_EMPLOYEE_ID = "employeeId";
    static final String ATTRIBUTE_TENANT_ID = "tenantId";
    static final String ATTRIBUTE_DEPARTMENT = "department";
    static final String ATTRIBUTE_BRANCH_CODE = "branchCode";
    static final String ATTRIBUTE_LAST_LOGIN_AT = "lastLoginAt";
    static final String ATTRIBUTE_LAST_LOGIN_IP = "lastLoginIp";
    static final String ATTRIBUTE_LAST_LOGIN_COUNTRY = "lastLoginCountry";
    static final String ATTRIBUTE_LAST_LOGIN_CITY = "lastLoginCity";
    static final String ATTRIBUTE_LAST_LOGIN_LATITUDE = "lastLoginLatitude";
    static final String ATTRIBUTE_LAST_LOGIN_LONGITUDE = "lastLoginLongitude";
    static final String ATTRIBUTE_LAST_LOGIN_DEVICE = "lastLoginDevice";
    static final String ATTRIBUTE_LAST_LOGIN_RISK_LEVEL = "lastLoginRiskLevel";
    static final String ATTRIBUTE_FAILED_LOGIN_COUNT = "failedLoginCount";
    static final String ATTRIBUTE_ACCOUNT_STATUS = "accountStatus";
    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    private static final String ACCOUNT_STATUS_LOCKED = "LOCKED";
    private static final int LOCK_THRESHOLD = 5;

    private final KeycloakUserClient keycloakUserClient;
    private final GeoLocationService geoLocationService;
    private final UserLoginAuditRepository userLoginAuditRepository;

    @Transactional
    public void updateLoginSecurityAttributes(String userId, LoginContext context) {
        ManagedUser user = keycloakUserClient.getUser(userId);
        Map<String, List<String>> currentAttributes = copyAttributes(user.getAttributes());
        ResolvedLoginContext resolvedContext = resolveContext(context);
        Map<String, List<String>> changedAttributes = new LinkedHashMap<>();

        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_AT, resolvedContext.loginTime().toString());
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_IP, resolvedContext.ipAddress());
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_COUNTRY, resolvedContext.country());
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_CITY, resolvedContext.city());
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_LATITUDE, stringify(resolvedContext.latitude()));
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_LONGITUDE, stringify(resolvedContext.longitude()));
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_DEVICE, resolvedContext.deviceFingerprint());
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_LAST_LOGIN_RISK_LEVEL, resolvedContext.riskLevel());

        int currentFailedLoginCount = parseInt(currentAttributes.get(ATTRIBUTE_FAILED_LOGIN_COUNT));
        int updatedFailedLoginCount = resolvedContext.status() == LoginStatus.FAILED
                ? currentFailedLoginCount + 1
                : currentFailedLoginCount;
        if (resolvedContext.status() == LoginStatus.FAILED) {
            putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_FAILED_LOGIN_COUNT, Integer.toString(updatedFailedLoginCount));
        }

        String accountStatus = resolveAccountStatus(firstValue(currentAttributes, ATTRIBUTE_ACCOUNT_STATUS), updatedFailedLoginCount);
        putIfChanged(changedAttributes, currentAttributes, ATTRIBUTE_ACCOUNT_STATUS, accountStatus);

        if (!changedAttributes.isEmpty()) {
            keycloakUserClient.updateUserAttributes(userId, changedAttributes);
            log.info(
                    "Updated {} security attributes for user '{}' (newLocation={}, newDevice={}, status={}).",
                    changedAttributes.size(),
                    userId,
                    isNewLocation(firstValue(currentAttributes, ATTRIBUTE_LAST_LOGIN_COUNTRY), resolvedContext.country()),
                    isNewDevice(firstValue(currentAttributes, ATTRIBUTE_LAST_LOGIN_DEVICE), resolvedContext.deviceFingerprint()),
                    resolvedContext.status());
        } else {
            log.info("No identity attribute changes detected for user '{}'; skipping provider update.", userId);
        }

        userLoginAuditRepository.save(UserLoginAuditEntity.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .loginTime(resolvedContext.loginTime())
                .ipAddress(resolvedContext.ipAddress())
                .country(resolvedContext.country())
                .city(resolvedContext.city())
                .latitude(resolvedContext.latitude())
                .longitude(resolvedContext.longitude())
                .deviceFingerprint(resolvedContext.deviceFingerprint())
                .riskLevel(context.getRiskLevel())
                .status(context.getStatus())
                .build());
    }

    public boolean isNewLocation(String previousCountry, String currentCountry) {
        if (currentCountry == null || currentCountry.isBlank()) {
            return false;
        }
        return previousCountry == null || previousCountry.isBlank() || !previousCountry.equalsIgnoreCase(currentCountry);
    }

    public boolean isNewDevice(String previousDevice, String currentDevice) {
        if (currentDevice == null || currentDevice.isBlank()) {
            return false;
        }
        return previousDevice == null || previousDevice.isBlank() || !previousDevice.equals(currentDevice);
    }

    private ResolvedLoginContext resolveContext(LoginContext context) {
        GeoLocation geoLocation = geoLocationService.getLocation(context.getIpAddress());
        OffsetDateTime loginTime = context.getLoginTime() == null ? OffsetDateTime.now(ZoneOffset.UTC) : context.getLoginTime();
        return new ResolvedLoginContext(
                context.getIpAddress(),
                firstNonBlank(context.getCountry(), geoLocation.getCountry()),
                firstNonBlank(context.getCity(), geoLocation.getCity()),
                context.getLatitude() != null ? context.getLatitude() : geoLocation.getLatitude(),
                context.getLongitude() != null ? context.getLongitude() : geoLocation.getLongitude(),
                context.getDeviceFingerprint(),
                loginTime,
                context.getRiskLevel().name(),
                context.getStatus());
    }

    private String resolveAccountStatus(String currentAccountStatus, int failedLoginCount) {
        if (failedLoginCount >= LOCK_THRESHOLD) {
            return ACCOUNT_STATUS_LOCKED;
        }
        if (currentAccountStatus == null || currentAccountStatus.isBlank()) {
            return ACCOUNT_STATUS_ACTIVE;
        }
        return currentAccountStatus;
    }

    private void putIfChanged(Map<String, List<String>> changedAttributes,
                              Map<String, List<String>> currentAttributes,
                              String key,
                              String newValue) {
        if (newValue == null || newValue.isBlank()) {
            return;
        }

        String existingValue = firstValue(currentAttributes, key);
        if (existingValue != null && existingValue.equals(newValue)) {
            return;
        }

        changedAttributes.put(key, List.of(newValue));
    }

    private Map<String, List<String>> copyAttributes(Map<String, List<String>> attributes) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (attributes == null) {
            return copy;
        }
        attributes.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return copy;
    }

    private String firstValue(Map<String, List<String>> attributes, String key) {
        List<String> values = attributes.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private int parseInt(List<String> values) {
        if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(values.get(0));
        } catch (NumberFormatException exception) {
            log.warn("Unable to parse failed login count '{}'; defaulting to 0.", values.get(0));
            return 0;
        }
    }

    private String stringify(Double value) {
        return value == null ? null : Double.toString(value);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private record ResolvedLoginContext(
            String ipAddress,
            String country,
            String city,
            Double latitude,
            Double longitude,
            String deviceFingerprint,
            OffsetDateTime loginTime,
            String riskLevel,
            LoginStatus status
    ) {
    }
}
