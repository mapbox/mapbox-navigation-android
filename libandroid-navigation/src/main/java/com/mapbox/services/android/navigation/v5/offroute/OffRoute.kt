package com.mapbox.services.android.navigation.v5.offroute

import android.location.Location
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

abstract class OffRoute {

    abstract fun isUserOffRoute(
        location: Location,
        routeProgress: RouteProgress,
        options: MapboxNavigationOptions
    ): Boolean
}
