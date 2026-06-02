package com.erainfotech.ums.client.keycloak;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedUser {

    private String id;
    private String username;
    private Map<String, List<String>> attributes;
}
