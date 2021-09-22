package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute

data class NavigationRoute(
    val routesResponse: DirectionsResponse?,
    val activeRoutes: List<DirectionsRoute>,
    val routeIndex: Int = 0,
    val initialLegIndex: Int = 0
) {
    fun primaryRoute(): DirectionsRoute? =
        routesResponse?.routes()?.getOrNull(routeIndex)

    fun routes(): List<DirectionsRoute> =
        routesResponse?.routes() ?: emptyList()
}
