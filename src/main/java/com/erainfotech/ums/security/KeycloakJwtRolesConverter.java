package com.erainfotech.ums.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

    private final Set<String> roleSourceClients;

    public KeycloakJwtRolesConverter(List<String> roleSourceClients) {
        this.roleSourceClients = roleSourceClients == null
                ? Collections.emptySet()
                : new HashSet<>(roleSourceClients);
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
        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptySet();
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
            if (!roleSourceClients.isEmpty() && !roleSourceClients.contains(entry.getKey())) {
                continue;
            }

            if (!(entry.getValue() instanceof Map<?, ?> clientAccess)) {
                continue;
            }

            Object roles = clientAccess.get("roles");
            if (!(roles instanceof List<?> roleList)) {
                continue;
            }

            for (Object role : roleList) {
                if (role instanceof String roleName) {
                    authorities.add(toAuthority(roleName));
                }
            }
        }

        return authorities;
    }

    private GrantedAuthority toAuthority(String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return new SimpleGrantedAuthority(normalized);
    }
}
