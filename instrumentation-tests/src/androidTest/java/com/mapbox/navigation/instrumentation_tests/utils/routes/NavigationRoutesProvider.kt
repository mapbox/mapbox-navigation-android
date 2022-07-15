package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin

object NavigationRoutesProvider {

    fun dc_very_short(context: Context): List<NavigationRoute> {
        val mockRoute = MockRoutesProvider.dc_very_short(context)
        return NavigationRoute.create(
            mockRoute.routeResponse,
            RouteOptions.builder()
                .coordinatesList(mockRoute.routeWaypoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build(),
            RouterOrigin.Custom()
        )
    }

    fun dc_very_short_two_legs(context: Context): List<NavigationRoute> {
        val mockRoute = MockRoutesProvider.dc_very_short_two_legs(context)
        return NavigationRoute.create(
            mockRoute.routeResponse,
            RouteOptions.builder()
                .coordinatesList(mockRoute.routeWaypoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build(),
            RouterOrigin.Custom()
        )
    }
}
