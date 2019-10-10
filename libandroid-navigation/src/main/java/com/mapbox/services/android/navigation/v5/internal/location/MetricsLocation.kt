package com.mapbox.services.android.navigation.v5.internal.location

import android.location.Location

class MetricsLocation(private val _location: Location?) {

        val location: Location
        get() {
            if (_location != null) {
                return _location
            }

            val metricLocation = Location("MetricsLocation")
            metricLocation.latitude = 0.0
            metricLocation.longitude = 0.0

            return metricLocation
        }
}
