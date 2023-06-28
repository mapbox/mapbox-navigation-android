package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RoutesInvalidatedParams

internal class RoutesRefreshAttemptProcessor(
    private val observersManager: RefreshObserversManager
) : RoutesRefreshAttemptListener {

    private val invalidatedRouteIds = mutableSetOf<String>()

    override fun onRoutesRefreshAttemptFinished(result: RoutesRefresherResult) {
        processInvalidatedRoutes(result)
    }

    private fun processInvalidatedRoutes(result: RoutesRefresherResult) {
        val invalidatedRoutes = mutableListOf<NavigationRoute>()
        if (result.primaryRouteRefresherResult.status == RouteRefresherStatus.INVALIDATED) {
            if (invalidatedRouteIds.add(result.primaryRouteRefresherResult.route.id)) {
                invalidatedRoutes.add(result.primaryRouteRefresherResult.route)
            }
        }
        result.alternativesRouteRefresherResults.forEach {
            if (it.status == RouteRefresherStatus.INVALIDATED) {
                if (invalidatedRouteIds.add(it.route.id)) {
                    invalidatedRoutes.add(it.route)
                }
            }
        }
        if (invalidatedRoutes.isNotEmpty()) {
            observersManager.onRoutesInvalidated(RoutesInvalidatedParams(invalidatedRoutes))
        }
    }
}
