package com.mapbox.navigation.core.infra.factories

import android.location.Location

fun createLocation(longitude: Double = 0.0, latitude: Double = 0.0) = Location("").apply {
    setLatitude(latitude)
    setLongitude(longitude)
}
