package com.erainfotech.ums.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {

    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
}
