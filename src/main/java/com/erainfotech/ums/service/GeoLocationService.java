package com.erainfotech.ums.service;

import com.erainfotech.ums.model.GeoLocation;

public interface GeoLocationService {

    GeoLocation getLocation(String ipAddress);
}
