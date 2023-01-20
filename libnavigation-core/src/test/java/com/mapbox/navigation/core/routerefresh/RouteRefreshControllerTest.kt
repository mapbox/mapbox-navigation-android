package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshControllerTest {

    private val plannedRouteRefreshController = mockk<PlannedRouteRefreshController>(relaxed = true)
    private val immediateRouteRefreshController =
        mockk<ImmediateRouteRefreshController>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val refreshObserversManager = mockk<RefreshObserversManager>(relaxed = true)
    private val resultProcessor = mockk<RouteRefresherResultProcessor>(relaxed = true)
    private val sut = RouteRefreshController(
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

        verify(exactly = 1) {
            refreshObserversManager.unregisterAllObservers()
            stateHolder.unregisterAllRouteRefreshStateObservers()
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

        sut.requestImmediateRouteRefresh(routes)

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
        val callback = slot<(RouteRefresherResult) -> Unit>()

        sut.requestImmediateRouteRefresh(routes)

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(RouteRefresherResult(true, mockk()))
        verify(exactly = 0) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithNonEmptyFailure() {
        val routes = listOf<NavigationRoute>(mockk())
        val callback = slot<(RouteRefresherResult) -> Unit>()

        sut.requestImmediateRouteRefresh(routes)

        verify(exactly = 1) {
            immediateRouteRefreshController.requestRoutesRefresh(routes, capture(callback))
        }
        callback.captured(RouteRefresherResult(false, mockk()))
        verify(exactly = 1) {
            plannedRouteRefreshController.resume()
        }
    }

    @Test
    fun requestImmediateRouteRefreshWithEmptyRoutes() {
        val routes = emptyList<NavigationRoute>()

        sut.requestImmediateRouteRefresh(routes)

        verify(exactly = 0) {
            plannedRouteRefreshController.pause()
            immediateRouteRefreshController.requestRoutesRefresh(routes, any())
            resultProcessor.reset()
        }
    }
}
