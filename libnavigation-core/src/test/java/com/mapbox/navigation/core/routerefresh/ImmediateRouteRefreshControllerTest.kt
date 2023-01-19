package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RouteProgressData
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ImmediateRouteRefreshControllerTest {

    private val routeRefresherExecutor = mockk<RouteRefresherExecutor>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val listener = mockk<RouteRefresherListener>(relaxed = true)
    private val clientCallback = mockk<(RouteRefresherResult) -> Unit>(relaxed = true)
    private val routes = listOf<NavigationRoute>(mockk())

    private val sut = ImmediateRouteRefreshController(
        routeRefresherExecutor,
        stateHolder,
        listener
    )

    @Test
    fun requestRoutesRefreshWithEmptyRoutes() {
        sut.requestRoutesRefresh(emptyList(), clientCallback)

        verify(exactly = 0) {
            clientCallback(any())
            routeRefresherExecutor.postRoutesToRefresh(any(), any())
        }
    }

    @Test
    fun requestRoutesRefreshPostsRefreshRequest() {
        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 1) { routeRefresherExecutor.postRoutesToRefresh(routes, any()) }
    }

    @Test
    fun routesRefreshStarted() {
        sut.requestRoutesRefresh(routes, clientCallback)
        val callback = interceptCallback()

        callback.onStarted()

        verify(exactly = 1) { stateHolder.onStarted() }
    }

    @Test
    fun routesRefreshFinishedSuccessfully() {
        sut.requestRoutesRefresh(routes, clientCallback)
        val callback = interceptCallback()
        val result = RouteRefresherResult(
            true,
            listOf(mockk(), mockk()),
            RouteProgressData(1, 2, 3)
        )

        callback.onResult(result)

        verify(exactly = 1) { stateHolder.onSuccess() }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
        verify(exactly = 1) { clientCallback(result) }
    }

    @Test
    fun routesRefreshFinishedWithFailure() {
        sut.requestRoutesRefresh(routes, clientCallback)
        val callback = interceptCallback()
        val result = RouteRefresherResult(
            false,
            listOf(mockk(), mockk()),
            RouteProgressData(1, 2, 3)
        )

        callback.onResult(result)

        verify(exactly = 1) { stateHolder.onFailure(null) }
        verify(exactly = 1) { clientCallback(result) }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
    }

    private fun interceptCallback(): RouteRefresherProgressCallback {
        val callbacks = mutableListOf<RouteRefresherProgressCallback>()
        verify { routeRefresherExecutor.postRoutesToRefresh(any(), capture(callbacks)) }
        return callbacks.last()
    }
}
