package com.erainfotech.ums.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String refreshToken,
        @NotBlank String clientId
) {
}
