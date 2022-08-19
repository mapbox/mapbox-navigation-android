package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.internal.CurrentIndices
import com.mapbox.navigation.base.route.NavigationRoute

internal data class RefreshedRouteInfo(
    val routes: List<NavigationRoute>,
    val usedIndicesSnapshot: CurrentIndices,
)
