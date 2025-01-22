package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshOnDemandIntegrationTest : RouteRefreshIntegrationTest() {

    @Test
    fun routeRefreshOnDemandDoesNotNotifyObserverBeforeTimeout() = runBlocking {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 100,
        )
        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        // should notify after 60_000 * 3
        testDispatcher.advanceTimeBy(179_000)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(0, refreshObserver.refreshes.size)
    }

    @Test
    fun routeRefreshOnDemandDoesNotNotifyObserverAfterTimeout() = runBlocking {
        val routes = setUpRoutes("route_response_single_route_refresh.json", responseDelay = 30_000)
        routeRefreshController = createRefreshController(60_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        testDispatcher.advanceTimeBy(80_000)
        routeRefreshController.requestImmediateRouteRefresh()
        testDispatcher.advanceTimeBy(10_000)
        assertEquals(0, refreshObserver.refreshes.size)
        testDispatcher.advanceTimeBy(20_000)
        assertEquals(1, refreshObserver.refreshes.size)
    }

    @Test
    fun successfulRefreshOnDemandWhenUpdatesArePaused() = runBlocking {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(1, refreshObserver.refreshes.size)
        routeRefreshController.pauseRouteRefreshes()
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(2, refreshObserver.refreshes.size)

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(3, refreshObserver.refreshes.size)

        routeRefreshController.resumeRouteRefreshes()
        testDispatcher.advanceTimeBy(10_000)
        assertEquals(3, refreshObserver.refreshes.size)
        testDispatcher.advanceTimeBy(20_000)
        assertEquals(4, refreshObserver.refreshes.size)
    }

    @Test
    fun failedRefreshOnDemandWhenUpdatesArePaused() = runBlocking {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )
        testDispatcher.advanceTimeBy(30_000)

        assertEquals(1, refreshObserver.refreshes.size)
        failRouteRefreshResponse()
        routeRefreshController.pauseRouteRefreshes()
        routeRefreshController.requestImmediateRouteRefresh()

        testDispatcher.advanceTimeBy(30_000)
        assertEquals(1, refreshObserver.refreshes.size)

        setUpRouteRefresh("route_response_single_route_refresh.json")
        routeRefreshController.resumeRouteRefreshes()
        testDispatcher.advanceTimeBy(30_000)
        assertEquals(2, refreshObserver.refreshes.size)
    }

    @Test
    fun routeRefreshOnDemandAfterAlternativesChangeBetweenAttempts() = runBlocking {
        val routes = setUpRoutes(
            fileName = "route_response_route_refresh_multileg.json",
            successfulAttemptNumber = 1,
        )
        val initialRoutes = routes.take(1)

        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(initialRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(59_000) // in-between attempts

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE),
        )

        routeRefreshController.requestImmediateRouteRefresh()
        testDispatcher.advanceTimeBy(29_000)

        assertEquals(1, refreshObserver.refreshes.size) // from refresh on-demand
        assertEquals(
            routes[0].id,
            refreshObserver.refreshes.first().primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            routes[1].id,
            refreshObserver.refreshes.first().alternativesRouteRefresherResults.first().route.id,
        )

        testDispatcher.advanceTimeBy(1_000)
        assertEquals(2, refreshObserver.refreshes.size) // from refresh on-demand + planned
        assertEquals(
            routes[0].id,
            refreshObserver.refreshes[1].primaryRouteRefresherResult.route.id,
        )
        assertEquals(
            routes[1].id,
            refreshObserver.refreshes[1].alternativesRouteRefresherResults.first().route.id,
        )

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot(),
        )
    }

    @Test
    fun routeRefreshOnDemandAfterReroute() = runBlocking {
        val routes = setUpRoutes(fileName = "route_response_route_refresh_multileg.json")

        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        testDispatcher.advanceTimeBy(29_000)

        val routesAfterReroute = setUpRoutes(fileName = "route_response_reroute_for_refresh.json")
        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(
                routesAfterReroute,
                emptyList(),
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            ),
        )

        routeRefreshController.requestImmediateRouteRefresh()

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
    fun routesAreUpdatedWhileRefreshOnDemandRequestIsInProgress() = runBlocking {
        val routes = setUpRoutes("route_response_route_refresh_multileg.json", responseDelay = 3000)
        val initialRoutes = routes.take(1)

        routeRefreshController = createRefreshController(100_000)
        routeRefreshController.registerRouteRefreshObserver(refreshObserver)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(initialRoutes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        routeRefreshController.requestImmediateRouteRefresh()

        routeRefreshController.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE),
        )

        testDispatcher.advanceTimeBy(6000) // response_delay x routes.size

        assertEquals(0, refreshObserver.refreshes.size)

        setUpRouteRefresh("route_response_route_refresh_multileg.json", responseDelay = 0)

        routeRefreshController.requestImmediateRouteRefresh()

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
