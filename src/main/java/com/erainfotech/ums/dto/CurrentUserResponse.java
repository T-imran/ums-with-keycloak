package com.erainfotech.ums.dto;

import java.util.Set;

public record CurrentUserResponse(
        String subject,
        String username,
        String email,
        Set<String> authorities
) {
}
