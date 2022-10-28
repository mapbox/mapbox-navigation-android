package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RouteRefreshRequestDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import java.util.Date

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        routeRefreshRequestDataProvider: RouteRefreshRequestDataProvider,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        routeRefreshRequestDataProvider,
        DirectionsRouteDiffProvider(),
        { Date() },
    )
}
