package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshCallback
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshError
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class PlannedRouteRefreshIntegrationTest : RouteRefreshIntegrationTest() {

    @Test
    fun routeRefreshMultipleAttemptsWhenAlternativesChange() = runBlocking {
        routeRefreshController = createRefreshController(30_000)
        val routes = setUpRoutes(
            "route_response_route_refresh_multileg.json",
            successfulAttemptNumber = 1,
        )
        val initialRoutes = routes.take(1)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(initialRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(50_000) // 30s refresh interval + 20s to be in-between

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE),
        )

        testDispatcher.advanceTimeBy(10_000)
        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            routes[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            routes[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun routeRefreshWhenRerouteToAlternative() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json")

        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(20_000)

        val routesAfterReroute = routes.asReversed()
        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(
                routesAfterReroute,
                emptyList(),
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            ),
        )

        testDispatcher.advanceTimeBy(40_000)
        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            routes[1].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun routeRefreshWhenRerouteToNewRoutes() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json")

        routeRefreshController = createRefreshController(100_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(30_000)

        val routesAfterReroute = setUpRoutes(fileName = "route_response_reroute_for_refresh.json")
        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(
                routesAfterReroute,
                emptyList(),
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            ),
        )

        testDispatcher.advanceTimeBy(70_000)
        assertEquals(0, refreshObserver.refreshes.size)

        testDispatcher.advanceTimeBy(30_000)

        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            routesAfterReroute[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            routesAfterReroute[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun routeRefreshWhenNewRoutesAreSet() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json")

        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(20_000)

        val newRoutes = setUpRoutes(fileName = "route_response_new_for_refresh.json")
        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(newRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(40_000)

        assertEquals(0, refreshObserver.refreshes.size)

        testDispatcher.advanceTimeBy(20_000)

        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            newRoutes[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            newRoutes[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun routeRefreshMultipleAttemptsWhenNewRoutesAreSet() = runBlocking {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 1,
        )

        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(80_000) // 60s refresh interval + 20s to be in-between

        val newRoutes = setUpRoutes(
            "route_response_new_for_refresh.json",
            successfulAttemptNumber = 0,
        )

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(newRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(40_000)
        assertEquals(0, refreshObserver.refreshes.size)

        testDispatcher.advanceTimeBy(20_000)

        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            newRoutes[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            newRoutes[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun offlineRouteTheRouterCanNeverRefreshStopsPlannedRefreshes() = runBlocking {
        // The harness has no progress data for alternatives, so only the primary route.
        val routes = createOfflineEvRoutes("route_response_route_refresh_multileg.json").take(1)
        every { router.supportsOnDeviceRefresh() } returns true
        every { router.getRouteRefresh(any(), any<RouteRefreshRequestData>(), any()) } answers {
            (thirdArg() as NavigationRouterRefreshCallback).onFailure(
                NavigationRouterRefreshError("can't refresh", permanent = true),
            )
            0
        }
        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        testDispatcher.advanceTimeBy(30_000 * 5)

        // One attempt fails, then nothing more: no retries and no expired-data clean-up.
        verify(exactly = 1) {
            router.getRouteRefresh(any(), any<RouteRefreshRequestData>(), any())
        }
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            stateObserver.getStatesSnapshot(),
        )
        assertEquals(0, refreshObserver.refreshes.size)
    }

    @Test
    fun offlineEvRouteIsRefreshedWhenTheDeviceCanRefreshIt() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json")
        val offlineRoutes = createOfflineEvRoutes("route_response_route_refresh_multileg.json")
        every { router.supportsOnDeviceRefresh() } returns true
        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(offlineRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        testDispatcher.advanceTimeBy(30_000)

        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            routes.map { it.id },
            refreshObserver.refreshes.first().let { result ->
                listOf(result.primaryRouteRefresherResult.route.id) +
                    result.alternativesRouteRefresherResults.map { it.route.id }
            },
        )
        assertEquals(true, refreshObserver.refreshes.first().anySuccess())
    }

    @Test
    fun routeRefreshWhenRoutesAreCleared() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json")

        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(20_000)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(
                emptyList(),
                emptyList(),
                RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            ),
        )

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(0, refreshObserver.refreshes.size)
        assertEquals(0, stateObserver.getStatesSnapshot().size)
    }

    @Test
    fun routesChangeToAlternativeWhenRequestIsInProgress() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json", responseDelay = 3000)
        val initialRoutes = routes.take(1)
        routeRefreshController = createRefreshController(40_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(initialRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(42_000) // start the refresh request

        // set alternatives
        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE),
        )

        testDispatcher.advanceTimeBy(3_000) // response delay
        assertEquals(1, refreshObserver.refreshes.size)
        assertEquals(
            routes[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            routes[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )
        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }
}
