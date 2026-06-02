package com.erainfotech.ums.service;

import com.erainfotech.ums.model.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultGeoLocationService implements GeoLocationService {

    @Override
    public GeoLocation getLocation(String ipAddress) {
        log.debug("No external GeoIP provider configured; returning empty location for IP '{}'.", ipAddress);
        return GeoLocation.builder().build();
    }
}
