package com.erainfotech.ums.client.keycloak;

import com.erainfotech.ums.config.IdentityAdminProperties;
import com.erainfotech.ums.exception.KeycloakCommunicationException;
import com.erainfotech.ums.exception.UserNotFoundException;
import jakarta.ws.rs.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminUserClient implements KeycloakUserClient {

    private final Keycloak identityProviderAdminClient;
    private final IdentityAdminProperties identityAdminProperties;

    @Override
    public ManagedUser getUser(String userId) {
        try {
            UserRepresentation userRepresentation = identityProviderAdminClient.realm(identityAdminProperties.realm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            if (userRepresentation == null) {
                throw new UserNotFoundException("Identity user not found: " + userId);
            }

            return ManagedUser.builder()
                    .id(userRepresentation.getId())
                    .username(userRepresentation.getUsername())
                    .attributes(copyAttributes(userRepresentation.getAttributes()))
                    .build();
        } catch (NotFoundException exception) {
            throw new UserNotFoundException("Identity user not found: " + userId, exception);
        } catch (RuntimeException exception) {
            log.error("Failed to fetch identity user '{}' from admin API.", userId, exception);
            throw new KeycloakCommunicationException("Failed to fetch identity user " + userId, exception);
        }
    }

    @Override
    public void updateUserAttributes(String userId, Map<String, List<String>> attributes) {
        try {
            UserRepresentation userRepresentation = identityProviderAdminClient.realm(identityAdminProperties.realm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            if (userRepresentation == null) {
                throw new UserNotFoundException("Identity user not found: " + userId);
            }

            Map<String, List<String>> mergedAttributes = copyAttributes(userRepresentation.getAttributes());
            mergedAttributes.putAll(attributes);
            userRepresentation.setAttributes(mergedAttributes);
            identityProviderAdminClient.realm(identityAdminProperties.realm()).users().get(userId).update(userRepresentation);
        } catch (NotFoundException exception) {
            throw new UserNotFoundException("Identity user not found: " + userId, exception);
        } catch (RuntimeException exception) {
            log.error("Failed to update identity user attributes for '{}'.", userId, exception);
            throw new KeycloakCommunicationException("Failed to update identity user attributes for " + userId, exception);
        }
    }

    private Map<String, List<String>> copyAttributes(Map<String, List<String>> attributes) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (attributes == null) {
            return copy;
        }
        attributes.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return copy;
    }
}
