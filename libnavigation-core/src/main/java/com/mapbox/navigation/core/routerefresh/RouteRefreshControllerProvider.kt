package com.mapbox.navigation.core.routerefresh

import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        directionsSession: DirectionsSession,
        tripSession: TripSession,
        logger: Logger
    ) = RouteRefreshController(
        directionsSession,
        tripSession,
        logger
    )
}
