package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import java.util.Date;

interface EnhancedLocationMediator {
    /**
     * Pass raw location to the core navigation
     *
     * @param location
     */
    void updateRawLocation(Location location);

    /**
     * Return improved location fix
     *
     * @param date             timestamp of the location update
     * @param lagMillis        lag in location updates
     * @param fallbackLocation default return if improved is unavailable
     * @return location fix
     */
    Location getLocation(Date date, long lagMillis, Location fallbackLocation);
}
