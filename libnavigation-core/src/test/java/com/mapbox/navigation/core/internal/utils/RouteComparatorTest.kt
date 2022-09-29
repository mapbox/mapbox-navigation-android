package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteComparatorTest {
    @Test
    fun test() {
        val rejectedRoutesTracker = createRejectedRoutesTracker()
        val routesUpdates = readRouteObserverResults("com.mapbox.navigation.core.internal.utils.similarroutes.munichnurberg")
        for (routesUpdate in routesUpdates) {
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {

            }
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
                val result = rejectedRoutesTracker.trackAlternatives(routesUpdate.navigationRoutes.drop(1))
                assertEquals(emptyList<NavigationRoute>(), result.untracked)
            }
        }
    }

    private fun createRejectedRoutesTracker() = RejectedRoutesTracker()
}