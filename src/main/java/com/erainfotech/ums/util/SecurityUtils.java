package com.erainfotech.ums.util;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentSubject() {
        Jwt jwt = currentJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    public static String currentUsername() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return null;
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        return Objects.requireNonNullElse(preferredUsername, jwt.getSubject());
    }

    public static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return jwt;
    }

    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }
}
