package com.erainfotech.ums.dto;

import com.erainfotech.ums.model.LoginContext;
import com.erainfotech.ums.model.LoginStatus;
import com.erainfotech.ums.model.RiskLevel;
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
public class LoginSecurityAttributesRequest {

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

    public LoginContext toLoginContext() {
        return LoginContext.builder()
                .ipAddress(ipAddress)
                .country(country)
                .city(city)
                .latitude(latitude)
                .longitude(longitude)
                .deviceFingerprint(deviceFingerprint)
                .loginTime(loginTime)
                .riskLevel(riskLevel)
                .status(status)
                .build();
    }
}
