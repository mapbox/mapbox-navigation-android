package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshOnDemandIntegrationTest : RouteRefreshIntegrationTest() {

    @Test
    fun routeRefreshOnDemandDoesNotNotifyObserverBeforeTimeout() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 100
        )
        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        // should notify after 60_000 * 3
        testDispatcher.advanceTimeBy(179_000)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(0, refreshObserver.refreshes.size)
    }

    @Test
    fun routeRefreshOnDemandDoesNotNotifyObserverAfterTimeout() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes("route_response_single_route_refresh.json", responseDelay = 30_000)
        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(80_000)
        routeRefreshController.requestImmediateRouteRefresh()
        testDispatcher.advanceTimeBy(10_000)
        assertEquals(0, refreshObserver.refreshes.size)
        testDispatcher.advanceTimeBy(20_000)
        assertEquals(1, refreshObserver.refreshes.size)
    }
}
