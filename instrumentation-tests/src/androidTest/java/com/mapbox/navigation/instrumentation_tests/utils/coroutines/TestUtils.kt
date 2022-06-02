package com.mapbox.navigation.instrumentation_tests.utils.coroutines

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.utils.routesUpdates
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val MAX_TIME_TO_UPDATE_ROUTE = 5_000L

suspend fun MapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes: List<NavigationRoute>) {
    withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
        coroutineScope {
            launch {
                if (routes.isEmpty()) {
                    waitForRoutesCleanup()
                } else {
                    waitForNewRoute()
                }
            }
            setNavigationRoutes(routes)
        }
    }
}

suspend fun MapboxNavigation.waitForNewRoute() {
    waitForRoutesUpdate(RoutesExtra.ROUTES_UPDATE_REASON_NEW)
}

suspend fun MapboxNavigation.waitForRoutesCleanup() {
    waitForRoutesUpdate(RoutesExtra.ROUTES_UPDATE_REASON_NEW)
}

private suspend fun MapboxNavigation.waitForRoutesUpdate(@RoutesExtra.RoutesUpdateReason reason: String) {
    routesUpdates()
        .filter { it.reason == reason}
        .first()
}
