package com.erainfotech.ums.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakJwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String resourceClientId;

    public KeycloakJwtRolesConverter(String resourceClientId) {
        this.resourceClientId = resourceClientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.addAll(extractRealmRoles(jwt));
        authorities.addAll(extractResourceRoles(jwt));
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptySet();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return Collections.emptySet();
        }

        return roleList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::toAuthority)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(resourceClientId)) {
            return Collections.emptySet();
        }

        Object clientAccessObj = resourceAccess.get(resourceClientId);
        if (!(clientAccessObj instanceof Map<?, ?> clientAccess)) {
            return Collections.emptySet();
        }

        Object roles = clientAccess.get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return Collections.emptySet();
        }

        return roleList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::toAuthority)
                .toList();
    }

    private GrantedAuthority toAuthority(String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return new SimpleGrantedAuthority(normalized);
    }
}
