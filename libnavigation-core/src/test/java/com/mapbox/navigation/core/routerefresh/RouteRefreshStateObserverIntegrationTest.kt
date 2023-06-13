package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class RouteRefreshStateObserverIntegrationTest : RouteRefreshIntegrationTest() {

    @Test
    fun emptyRoutesOnDemandRefreshTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(100000)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        routeRefreshController.requestPlannedRouteRefresh(emptyList())

        testDispatcher.advanceTimeBy(100000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshOnDemandForInvalidRoutes() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            enableRefresh = false
        )
        routeRefreshController = createRefreshController(50000)

        routeRefreshController.requestPlannedRouteRefresh(routes)

        testDispatcher.advanceTimeBy(50000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun invalidRoutesRefreshTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            enableRefresh = false
        )
        routeRefreshController = createRefreshController(100000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)

        testDispatcher.advanceTimeBy(100000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun successfulRefreshTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(100000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(100000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun notStartedUntilTimeElapses() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(50000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(49999)

        assertEquals(
            emptyList<String>(),
            stateObserver.getStatesSnapshot()
        )

        testDispatcher.advanceTimeBy(1)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun successfulFromSecondAttemptRefreshTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 1
        )
        routeRefreshController = createRefreshController(50000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(100000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun threeFailedAttemptsThenSuccessfulRefreshTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 3
        )
        routeRefreshController = createRefreshController(40_000)

        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(120_000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun successfulRouteRefreshOnDemandTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes("route_response_single_route_refresh.json")
        routeRefreshController = createRefreshController(100_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun failedRouteRefreshOnDemandTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 1
        )
        routeRefreshController = createRefreshController(100_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun multipleRouteRefreshesOnDemandTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            responseDelay = 4000
        )
        routeRefreshController = createRefreshController(200_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        updateProgressWithGeometryIndex(1)
        routeRefreshController.requestImmediateRouteRefresh()
        updateProgressWithGeometryIndex(2)
        testDispatcher.advanceTimeBy(2_000)
        routeRefreshController.requestImmediateRouteRefresh()
        updateProgressWithGeometryIndex(3)
        routeRefreshController.requestImmediateRouteRefresh()
        updateProgressWithGeometryIndex(4)
        routeRefreshController.requestImmediateRouteRefresh()
        testDispatcher.advanceTimeBy(2_000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot()
        )
        verify(exactly = 2) { router.getRouteRefresh(any(), any<RouteRefreshRequestData>(), any()) }
        verify {
            router.getRouteRefresh(
                any(),
                match<RouteRefreshRequestData> { it.routeGeometryIndex == 1 },
                any()
            )
            router.getRouteRefresh(
                any(),
                match<RouteRefreshRequestData> { it.routeGeometryIndex == 4 },
                any()
            )
        }
    }

    @Test
    fun routeRefreshOnDemandFailsThenPlannedTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 1
        )
        routeRefreshController = createRefreshController(50_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        routeRefreshController.requestImmediateRouteRefresh()

        testDispatcher.advanceTimeBy(50_000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshOnDemandFailsBetweenPlannedAttemptsTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 2
        )
        routeRefreshController = createRefreshController(50_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(50_000)
        routeRefreshController.requestImmediateRouteRefresh()
        testDispatcher.advanceTimeBy(50_000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshDoesNotDispatchCancelledStateOnDestroyTest() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 2
        )
        routeRefreshController = createRefreshController(50_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)

        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(80_000)

        routeRefreshController.destroy()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
            ),
            stateObserver.getStatesSnapshot()
        )
    }

    @Test
    fun pausingRoutesRefreshAddsCancelledState() = coroutineRule.runBlockingTest {
        val routes = setUpRoutes(
            "route_response_single_route_refresh.json",
            successfulAttemptNumber = 1,
        )

        routeRefreshController = createRefreshController(30_000)
        routeRefreshController.registerRouteRefreshStateObserver(stateObserver)
        routeRefreshController.requestPlannedRouteRefresh(routes)
        testDispatcher.advanceTimeBy(40_000)

        routeRefreshController.pauseRouteRefreshes()
        routeRefreshController.resumeRouteRefreshes()
        testDispatcher.advanceTimeBy(30_000)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            stateObserver.getStatesSnapshot()
        )
    }
}
