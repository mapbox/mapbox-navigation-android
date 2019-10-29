package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.navigation.model.RouteProgress

data class TripStatus(
    val enhancedLocation: Location,
    val routeProgress: RouteProgress
)
