package com.mapbox.services.android.navigation.v5.internal.location

import android.location.Location

class MetricsLocation(private val _location: Location?) {

    val location: Location by lazy {
        _location ?: Location("MetricsLocation").also { location ->
            location.latitude = 0.0
            location.longitude = 0.0
        }
    }
}
