package com.mapbox.navigation.core.testutil

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.IgnoredRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult

fun createRoutesUpdatedResult(
    routes: List<NavigationRoute>,
    @RoutesExtra.RoutesUpdateReason reason: String,
    ignoredRoutes: List<IgnoredRoute> = emptyList(),
): RoutesUpdatedResult = RoutesUpdatedResult(routes, ignoredRoutes, reason)
