package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
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
        resultProcessor
    )

    @Test
    fun registerRouteRefreshObserver() {
        val observer = mockk<RouteRefreshObserver>()

        sut.registerRouteRefreshObserver(observer)

        verify(exactly = 1) { refreshObserversManager.registerObserver(observer) }
    }

    @Test
    fun unregisterRouteRefreshObserver() {
        val observer = mockk<RouteRefreshObserver>()

        sut.unregisterRouteRefreshObserver(observer)

        verify(exactly = 1) { refreshObserversManager.unregisterObserver(observer) }
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
    fun destroy() {
        sut.destroy()

        verifyOrder {
            refreshObserversManager.unregisterAllObservers()
            stateHolder.unregisterAllRouteRefreshStateObservers()
            job.cancel()
        }
    }

    @Test
    fun requestPlannedRouteRefreshWithNonEmptyRoutes() {
        val routes = listOf<NavigationRoute>(mockk())

        sut.requestPlannedRouteRefresh(routes)

        verify(exactly = 1) {
            resultProcessor.reset()
            plannedRouteRefreshController.startRoutesRefreshing(routes)
        }
    }

    @Test
    fun requestPlannedRouteRefreshWithEmptyRoutes() {
        val routes = emptyList<NavigationRoute>()

        sut.requestPlannedRouteRefresh(routes)

        verify(exactly = 1) {
            resultProcessor.reset()
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
        val callback = slot<(Expected<String, RouteRefresherResult>) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(ExpectedFactory.createValue(RouteRefresherResult(true, mockk())))
        verify(exactly = 0) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyFailure() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(Expected<String, RouteRefresherResult>) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(ExpectedFactory.createValue(RouteRefresherResult(false, mockk())))
        verify(exactly = 1) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyError() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(Expected<String, RouteRefresherResult>) -> Unit>()
        every { plannedRouteRefreshController.routesToRefresh } returns routes

        sut.requestImmediateRouteRefresh()

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(ExpectedFactory.createError("error"))
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
}
