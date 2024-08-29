package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RoutesInvalidatedObserver
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Job
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshControllerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val plannedRouteRefreshController = mockk<PlannedRouteRefreshController>(relaxed = true)
    private val immediateRouteRefreshController =
        mockk<ImmediateRouteRefreshController>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val refreshObserversManager = mockk<RefreshObserversManager>(relaxed = true)
    private val resultProcessor = mockk<RouteRefresherResultProcessor>(relaxed = true)
    private val job = mockk<Job>(relaxed = true)
    private val sut = RouteRefreshController(
        job,
        plannedRouteRefreshController,
        immediateRouteRefreshController,
        stateHolder,
        refreshObserversManager,
        resultProcessor,
    )

    @Test
    fun registerRouteRefreshObserver() {
        val observer = mockk<RouteRefreshObserver>()

        sut.registerRouteRefreshObserver(observer)

        verify(exactly = 1) { refreshObserversManager.registerRefreshObserver(observer) }
    }

    @Test
    fun unregisterRouteRefreshObserver() {
        val observer = mockk<RouteRefreshObserver>()

        sut.unregisterRouteRefreshObserver(observer)

        verify(exactly = 1) { refreshObserversManager.unregisterRefreshObserver(observer) }
    }

    @Test
    fun registerRouteRefreshStatesObserver() {
        val observer = mockk<RouteRefreshStatesObserver>()

        sut.registerRouteRefreshStateObserver(observer)

        verify(exactly = 1) { stateHolder.registerRouteRefreshStateObserver(observer) }
    }

    @Test
    fun unregisterRouteRefreshStatesObserver() {
        val observer = mockk<RouteRefreshStatesObserver>()

        sut.unregisterRouteRefreshStateObserver(observer)

        verify(exactly = 1) { stateHolder.unregisterRouteRefreshStateObserver(observer) }
    }

    @Test
    fun registerRoutesInvalidatedObserver() {
        val observer = mockk<RoutesInvalidatedObserver>()

        sut.registerRoutesInvalidatedObserver(observer)

        verify(exactly = 1) { refreshObserversManager.registerInvalidatedObserver(observer) }
    }

    @Test
    fun unregisterRoutesInvalidatedObserver() {
        val observer = mockk<RoutesInvalidatedObserver>()

        sut.unregisterRoutesInvalidatedObserver(observer)

        verify(exactly = 1) { refreshObserversManager.unregisterInvalidatedObserver(observer) }
    }

    @Test
    fun destroy() {
        sut.destroy()

        verifyOrder {
            refreshObserversManager.unregisterAllObservers()
            stateHolder.unregisterAllRouteRefreshStateObservers()
            job.cancel()
        }
    }

    @Test
    fun onRoutesChangedWithNonEmptyRoutesNew() {
        val routes = listOf<NavigationRoute>(mockk())

        sut.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_NEW),
        )

        verify(exactly = 1) {
            resultProcessor.reset()
            plannedRouteRefreshController.startRoutesRefreshing(routes)
            immediateRouteRefreshController.cancel()
        }
    }

    @Test
    fun onRoutesChangedWithNonEmptyRoutesRefresh() {
        val routes = listOf<NavigationRoute>(mockk())

        sut.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_REFRESH),
        )

        verify(exactly = 0) {
            resultProcessor.reset()
            plannedRouteRefreshController.startRoutesRefreshing(any())
            immediateRouteRefreshController.cancel()
        }
    }

    @Test
    fun onRoutesChangedWithEmptyRoutes() {
        val routes = emptyList<NavigationRoute>()

        sut.onRoutesChanged(
            RoutesUpdatedResult(routes, emptyList(), RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP),
        )

        verify(exactly = 1) {
            resultProcessor.reset()
            immediateRouteRefreshController.cancel()
            plannedRouteRefreshController.startRoutesRefreshing(routes)
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyRoutes() {
        val routes = listOf<NavigationRoute>(mockk())
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            plannedRouteRefreshController.pause()
        }
        verify(exactly = 0) {
            plannedRouteRefreshController.resume()
        }
        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, any())
        }
        verify(exactly = 0) { resultProcessor.reset() }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptySuccess() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(RoutesRefresherExecutorResult) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(
            RoutesRefresherExecutorResult.Finished(
                mockk { every { anySuccess() } returns true },
            ),
        )
        verify(exactly = 1) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyFailure() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(RoutesRefresherExecutorResult) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(
            RoutesRefresherExecutorResult.Finished(
                mockk {
                    every { anySuccess() } returns false
                },
            ),
        )
        verify(exactly = 1) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyError() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(RoutesRefresherExecutorResult) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(RoutesRefresherExecutorResult.ReplacedByNewer)
        verify(exactly = 0) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithEmptyRoutes() {
        every { plannedRouteRefreshController.routesToRefresh } returns emptyList()

        sut.requestImmediateRouteRefresh()

        verify(exactly = 0) {
            plannedRouteRefreshController.pause()
            immediateRouteRefreshController.requestRoutesRefresh(any(), any())
            resultProcessor.reset()
        }
        verifyOrder {
            stateHolder.onStarted()
            stateHolder.onFailure("No routes to refresh")
        }
        verify(exactly = 1) {
            logger.logI("No routes to refresh", "RouteRefreshController")
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNullRoutes() {
        every { plannedRouteRefreshController.routesToRefresh } returns null

        sut.requestImmediateRouteRefresh()

        verify(exactly = 0) {
            plannedRouteRefreshController.pause()
            immediateRouteRefreshController.requestRoutesRefresh(any(), any())
            resultProcessor.reset()
        }
    }

    @Test
    fun pauseRouteRefreshes() {
        sut.pauseRouteRefreshes()

        verify(exactly = 1) {
            plannedRouteRefreshController.pause()
        }
    }

    @Test
    fun resumeRouteRefreshes() {
        sut.resumeRouteRefreshes()

        verify(exactly = 1) {
            plannedRouteRefreshController.resume()
        }
    }
}
