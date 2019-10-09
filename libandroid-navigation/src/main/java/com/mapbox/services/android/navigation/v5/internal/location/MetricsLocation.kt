package com.mapbox.services.android.navigation.v5.internal.location

import android.location.Location

class MetricsLocation(internal val location: Location?) {

    fun getLocation(): Location {
        if (location != null) {
            return location
        }

        val metricLocation = Location("MetricsLocation")
        metricLocation.latitude = 0.0
        metricLocation.longitude = 0.0

        return metricLocation
    }
}
