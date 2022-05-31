package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import java.util.Date

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        tripSession: TripSession,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        { tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0 },
        DirectionsRouteDiffProvider(),
        { Date() }
    )
}
