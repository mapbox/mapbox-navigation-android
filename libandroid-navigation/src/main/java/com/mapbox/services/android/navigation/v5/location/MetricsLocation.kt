package com.mapbox.services.android.navigation.v5.location

import android.location.Location

class MetricsLocation(private val location: Location?) {

    fun getLocation() =
            when (location == null) {
                true -> {
                    val metricLocation = Location("MetricsLocation")
                    metricLocation.latitude = 0.0
                    metricLocation.longitude = 0.0
                    metricLocation
                }
                else -> {
                    location
                }
            }
}
