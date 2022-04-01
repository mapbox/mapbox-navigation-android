package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.trip.session.MapboxTripSession
import com.mapbox.navigation.utils.internal.ThreadController

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: MapboxDirectionsSession,
        tripSession: MapboxTripSession,
        threadController: ThreadController,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        tripSession,
        threadController,
    )
}
