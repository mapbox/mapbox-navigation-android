package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.location.Location
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

interface NavigationMetricListener {

    fun onRouteProgressUpdate(routeProgress: RouteProgress)

    fun onOffRouteEvent(offRouteLocation: Location)

    fun onArrival(routeProgress: RouteProgress)
}
