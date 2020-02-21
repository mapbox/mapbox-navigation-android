package com.mapbox.navigation.core.metrics

import android.location.Location
import com.mapbox.navigation.base.trip.model.RouteProgress

interface NavigationMetricListener {

    fun onRouteProgressUpdate(routeProgress: RouteProgress)

    fun onOffRouteEvent(offRouteLocation: Location)

    fun onArrival(routeProgress: RouteProgress)
}
