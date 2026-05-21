package com.erainfotech.ums.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String password,
        boolean temporary
) {
}
