package com.mapbox.navigation.core.telemetry

import android.location.Location

class MetricsLocation(private val _location: Location?) {

    companion object {
        internal const val PROVIDER = "MetricsLocation"
    }

    val location: Location by lazy {
        _location ?: Location(PROVIDER).also { location ->
            location.latitude = 0.0
            location.longitude = 0.0
        }
    }
}
