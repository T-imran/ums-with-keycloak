package com.erainfotech.ums.client.keycloak;

import java.util.List;
import java.util.Map;

public interface KeycloakUserClient {

    ManagedUser getUser(String userId);

    void updateUserAttributes(String userId, Map<String, List<String>> attributes);
}
