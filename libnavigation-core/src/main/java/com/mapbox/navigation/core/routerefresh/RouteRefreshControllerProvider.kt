package com.mapbox.navigation.core.routerefresh

import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.ThreadController

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        tripSession: TripSession,
        logger: Logger,
        threadController: ThreadController,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        tripSession,
        logger,
        threadController,
    )
}
