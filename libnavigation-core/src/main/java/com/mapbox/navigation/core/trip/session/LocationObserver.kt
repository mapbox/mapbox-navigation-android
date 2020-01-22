package com.mapbox.navigation.core.trip.session

import android.location.Location

interface LocationObserver {
    fun onRawLocationChanged(rawLocation: Location)
    fun onEnhancedLocationChanged(enhancedLocation: Location)
}
