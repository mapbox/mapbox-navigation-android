package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.PrimaryRouteProgressDataProvider
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.routealternatives.AlternativeMetadataProvider
import java.util.Date

internal object RouteRefreshControllerProvider {

    fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        primaryRouteProgressDataProvider: PrimaryRouteProgressDataProvider,
        alternativeMetadataProvider: AlternativeMetadataProvider,
        evDynamicDataHolder: EVDynamicDataHolder,
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        RoutesProgressDataProvider(primaryRouteProgressDataProvider, alternativeMetadataProvider),
        EVRefreshDataProvider(evDynamicDataHolder),
        DirectionsRouteDiffProvider(),
        { Date() },
    )
}
