package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.RouteProgressData

internal data class RoutesRefreshData(
    val primaryRoute: NavigationRoute,
    val primaryRouteProgressData: RouteProgressData,
    val alternativeRoutesProgressData: List<Pair<NavigationRoute, RouteProgressData?>>
) {
    val allRoutesRefreshData = listOf(primaryRoute to primaryRouteProgressData) +
        alternativeRoutesProgressData
}

internal class RoutesRefreshDataProvider(
    private val routesProgressDataProvider: RoutesProgressDataProvider,
) {

    /**
     * Retrieved progress data for passed routes.
     *
     * @throws IllegalArgumentException if routes are empty
     */
    @Throws(IllegalArgumentException::class)
    suspend fun getRoutesRefreshData(
        routes: List<NavigationRoute>
    ): RoutesRefreshData {
        if (routes.isEmpty()) {
            throw IllegalArgumentException("Routes must not be empty")
        }
        val routesProgressData = routesProgressDataProvider
            .getRouteRefreshRequestDataOrWait()
        val primary = routes.first()
        val alternatives = routes.drop(1)
        val alternativeRouteProgressDatas = alternatives.map { route ->
            route to routesProgressData.alternatives[route.id]
        }
        return RoutesRefreshData(primary, routesProgressData.primary, alternativeRouteProgressDatas)
    }
}
