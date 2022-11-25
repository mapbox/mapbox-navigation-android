package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RouteProgressDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import java.util.Date

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        routeProgressDataProvider: RouteProgressDataProvider,
        evDynamicDataHolder: EVDynamicDataHolder,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        routeProgressDataProvider,
        EVRefreshDataProvider(evDynamicDataHolder),
        DirectionsRouteDiffProvider(),
        { Date() },
    )
}
