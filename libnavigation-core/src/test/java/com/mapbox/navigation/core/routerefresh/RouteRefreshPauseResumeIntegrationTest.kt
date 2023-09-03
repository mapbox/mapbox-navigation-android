package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshPauseResumeIntegrationTest : RouteRefreshIntegrationTest() {

    @Test
    fun pause_and_resume_refreshes() = runBlocking {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(refreshInterval = 60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(60_000)

        assertEquals(1, refreshObserver.refreshes.size)

        routeRefreshController.pauseRouteRefreshes()
        testDispatcher.advanceTimeBy(60_000)

        assertEquals(1, refreshObserver.refreshes.size)

        routeRefreshController.resumeRouteRefreshes()
        testDispatcher.advanceTimeBy(60_000)

        assertEquals(2, refreshObserver.refreshes.size)

        routeRefreshController.destroy()
    }

    @Test
    fun request_new_routes_planned_refresh_when_refresh_is_paused() = runBlocking {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(refreshInterval = 60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(60_000)

        assertEquals(1, refreshObserver.refreshes.size)

        routeRefreshController.pauseRouteRefreshes()
        testDispatcher.advanceTimeBy(90_000)

        assertEquals(1, refreshObserver.refreshes.size)

        val newRoutes = setUpRoutes("route_response_route_refresh_multileg.json")
        routeRefreshController.requestPlannedRouteRefresh(newRoutes)

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(1, refreshObserver.refreshes.size)

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(2, refreshObserver.refreshes.size)
    }
}
