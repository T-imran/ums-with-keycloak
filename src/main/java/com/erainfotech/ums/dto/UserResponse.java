package com.erainfotech.ums.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<String> roles,
        Map<String, List<String>> attributes
) {
}
