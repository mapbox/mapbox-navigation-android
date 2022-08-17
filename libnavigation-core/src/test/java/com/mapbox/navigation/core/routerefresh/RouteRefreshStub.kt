package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.CurrentIndices
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CurrentIndicesFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouterFactory.buildNavigationRouterRefreshError
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RouteRefreshStub : RouteRefresh {

    private var requestId = 0L
    private val handlers = mutableMapOf<String, RouteRefreshHandler>()

    override fun requestRouteRefresh(
        route: NavigationRoute,
        currentIndices: CurrentIndices,
        callback: NavigationRouterRefreshCallback
    ): Long {
        val currentRequestId = requestId++
        val handler = handlers[route.id]
        if (handler != null) {
            handler(route, currentIndices, callback)
        } else {
            callback.onFailure(buildNavigationRouterRefreshError("handle isn't configured yet"))
        }

        return currentRequestId
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
    }

    /***
     * Tne next route refresh requests will return the actual routes
     */
    fun setRefreshedRoute(refreshedRoute: NavigationRoute) {
        handlers[refreshedRoute.id] = { _, _, callback ->
            // TODO: refresh legs only from the passed index
            callback.onRefreshReady(refreshedRoute)
        }
    }

    /***
     * The next route refresh requests for the given route route will fail
     */
    fun failRouteRefresh(navigationRouteId: String) {
        handlers[navigationRouteId] = { _, _, callback ->
            callback.onFailure(
                buildNavigationRouterRefreshError(
                    "Failed by RouteRefreshStub#failPendingRefreshRequest"
                )
            )
        }
    }

    fun doNotRespondForRouteRefresh(navigationRouteId: String) {
        handlers[navigationRouteId] = { _, _, _ -> }
    }
}

private typealias RouteRefreshHandler = (
    route: NavigationRoute,
    currentIndices: CurrentIndices,
    callback: NavigationRouterRefreshCallback
) -> Unit

class RouteRefreshStubTest {

    @Test
    fun `refresh fails if no route`() {
        val stub = RouteRefreshStub()
        val callback = mockk<NavigationRouterRefreshCallback>(relaxed = true)

        stub.requestRouteRefresh(
            createNavigationRoute(),
            CurrentIndicesFactory.createIndices(0, 0, null),
            callback
        )

        verify(exactly = 1) { callback.onFailure(any()) }
        verify(exactly = 0) { callback.onRefreshReady(any()) }
    }

    @Test
    fun `route successfully refreshed if refreshed was set`() {
        val stub = RouteRefreshStub()
        val originalRoute = createNavigationRoute(
            createDirectionsRoute(
                duration = 1.0, requestUuid = "test"
            )
        )
        val refreshed = createNavigationRoute(
            createDirectionsRoute(
                duration = 2.0, requestUuid = "test"
            )
        )
        stub.setRefreshedRoute(refreshed)

        val callback = mockk<NavigationRouterRefreshCallback>(relaxed = true)
        stub.requestRouteRefresh(
            originalRoute,
            CurrentIndicesFactory.createIndices(0, 0, null),
            callback
        )

        verify(exactly = 1) { callback.onRefreshReady(refreshed) }
        verify(exactly = 0) { callback.onFailure(any()) }
    }

    @Test
    fun `refresh fails if stub was asked for`() {
        val testRoute = createNavigationRoute(
            createDirectionsRoute(requestUuid = "test-fail")
        )
        val stub = RouteRefreshStub().apply {
            setRefreshedRoute(testRoute) // make sure that it overrides old setup
            failRouteRefresh(testRoute.id)
        }
        val callback = mockk<NavigationRouterRefreshCallback>(relaxed = true)

        stub.requestRouteRefresh(
            testRoute,
            CurrentIndicesFactory.createIndices(0, 0, null),
            callback
        )

        verify(exactly = 1) { callback.onFailure(any()) }
        verify(exactly = 0) { callback.onRefreshReady(any()) }
    }

    @Test
    fun `refresh won't respond if stub was asked for`() {
        val testRoute = createNavigationRoute(
            createDirectionsRoute(requestUuid = "test-fail")
        )
        val stub = RouteRefreshStub().apply {
            setRefreshedRoute(testRoute) // make sure that it overrides old setup
            doNotRespondForRouteRefresh(testRoute.id)
        }
        val callback = mockk<NavigationRouterRefreshCallback>(relaxed = true)

        stub.requestRouteRefresh(
            testRoute,
            CurrentIndicesFactory.createIndices(0, 0, null),
            callback
        )

        verify(exactly = 0) { callback.onFailure(any()) }
        verify(exactly = 0) { callback.onRefreshReady(any()) }
    }
}
