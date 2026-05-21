package com.erainfotech.ums.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRealmRoleRequest(
        @NotBlank String name,
        String description
) {
}
