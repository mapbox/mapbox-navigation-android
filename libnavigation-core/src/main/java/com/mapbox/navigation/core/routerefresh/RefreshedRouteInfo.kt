package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RouteProgressData

internal data class RefreshedRouteInfo(
    val routes: List<NavigationRoute>,
    val routeProgressData: RouteProgressData,
)
