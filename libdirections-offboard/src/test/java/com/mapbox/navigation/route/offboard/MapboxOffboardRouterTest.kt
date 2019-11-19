package com.mapbox.navigation.route.offboard

import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.callback.RouteRetrieveCallback
import com.mapbox.navigation.route.offboard.router.NavigationRoute
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MapboxOffboardRouterTest {

    private lateinit var offboardRouter: MapboxOffboardRouter

    @Before
    fun setUp() {
        offboardRouter = MapboxOffboardRouter(mockk(), "pk_token")
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(offboardRouter)
    }

    @Test
    fun getRoute_NavigationRouteGetRouteCalled() {
        val navigationRoute = mockk<NavigationRoute>(relaxed = true)
        offboardRouter.setNavigationRoute(navigationRoute)
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val routeRetrieveCallback = RouteRetrieveCallback(routerCallback)
        offboardRouter.setRouteRetrieveCallback(routeRetrieveCallback)

        offboardRouter.getRoute(mockk(), mockk(), mockk(), routerCallback)

        verify { navigationRoute.getRoute(routeRetrieveCallback) }
    }

    @Test
    fun cancel_NavigationRouteCancelCallCalled() {
        val navigationRoute = mockk<NavigationRoute>(relaxed = true)
        offboardRouter.setNavigationRoute(navigationRoute)

        offboardRouter.cancel()

        verify { navigationRoute.cancelCall() }
    }
}
