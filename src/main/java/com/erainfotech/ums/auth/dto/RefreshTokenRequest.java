package com.erainfotech.ums.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank String refreshToken,
        @NotBlank String clientId
) {
}
