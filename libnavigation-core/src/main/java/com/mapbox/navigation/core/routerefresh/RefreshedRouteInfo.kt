package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.CurrentIndices
import com.mapbox.navigation.base.route.NavigationRoute

internal data class RefreshedRouteInfo(
    val routes: List<NavigationRoute>,
    val usedIndicesSnapshot: CurrentIndices,
)
