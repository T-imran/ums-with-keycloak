package com.erainfotech.ums.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginContext {

    @NotBlank
    private String ipAddress;
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
    @NotBlank
    private String deviceFingerprint;
    private OffsetDateTime loginTime;
    @NotNull
    private RiskLevel riskLevel;
    @NotNull
    private LoginStatus status;
}
