package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.NavigationComponentProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptions = mockk(relaxUnitFun = true)
    private val routerCallback: RouterCallback = mockk(relaxUnitFun = true)
    private val routesRefreshRequestCallback: RouteRefreshCallback = mockk(relaxUnitFun = true)
    private val observer: RoutesObserver = mockk(relaxUnitFun = true)
    private val route: DirectionsRoute = mockk(relaxUnitFun = true)
    private val routes: List<DirectionsRoute> = listOf(route)
    private lateinit var routeCallback: RouterCallback
    private lateinit var refreshCallback: RouteRefreshCallback

    private val routeRequestId = 1L
    private val routeRefreshRequestId = 2L

    @Before
    fun setUp() {
        val routeOptionsBuilder: RouteOptions.Builder = mockk(relaxUnitFun = true)
        every { routeOptionsBuilder.waypointIndices(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.waypointNames(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.waypointTargets(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.build() } returns routeOptions
        every { routeOptions.toBuilder() } returns routeOptionsBuilder
        every { routeOptions.waypointIndices() } returns ""
        every { routeOptions.waypointNames() } returns ""
        every { routeOptions.waypointTargets() } returns ""
        val routeBuilder: DirectionsRoute.Builder = mockk(relaxUnitFun = true)
        every { route.toBuilder() } returns routeBuilder
        every { routeBuilder.routeOptions(any()) } returns routeBuilder
        every { routeBuilder.build() } returns route

        val routeListener = slot<RouterCallback>()
        val refreshListener = slot<RouteRefreshCallback>()
        every { router.getRoute(routeOptions, capture(routeListener)) } answers {
            routeCallback = routeListener.captured
            routeRequestId
        }
        every { router.getRouteRefresh(route, 0, capture(refreshListener)) } answers {
            refreshCallback = refreshListener.captured
            routeRefreshRequestId
        }
        every { routes[0].routeOptions() } returns routeOptions
        mockkObject(NavigationComponentProvider)
        every { routerCallback.onRoutesReady(any(), any()) } answers {
            this.value
        }
        session = MapboxDirectionsSession(router)
    }

    @Test
    fun initialState() {
        assertNull(session.getPrimaryRouteOptions())
        assertEquals(session.routes, emptyList<DirectionsRoute>())
    }

    @Test
    fun `route response - success`() {
        val mockOrigin = mockk<RouterOrigin>()
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onRoutesReady(routes, mockOrigin)

        verify(exactly = 1) { routerCallback.onRoutesReady(routes, mockOrigin) }
    }

    @Test
    fun `route request returns id`() {
        assertEquals(
            1L,
            session.requestRoutes(routeOptions, routerCallback)
        )
    }

    @Test
    fun `route response - failure`() {
        val reasons: List<RouterFailure> = listOf(mockk())
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onFailure(reasons, routeOptions)

        verify(exactly = 1) {
            routerCallback.onFailure(reasons, routeOptions)
        }
    }

    @Test
    fun `route response - canceled`() {
        val mockOrigin = mockk<RouterOrigin>()
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onCanceled(routeOptions, mockOrigin)

        verify(exactly = 1) {
            routerCallback.onCanceled(routeOptions, mockOrigin)
        }
    }

    @Test
    fun `route refresh response - success`() {
        session.requestRouteRefresh(route, 0, routesRefreshRequestCallback)
        refreshCallback.onRefresh(route)

        verify(exactly = 1) { routesRefreshRequestCallback.onRefresh(route) }
    }

    @Test
    fun `route refresh request returns id`() {
        assertEquals(
            2L,
            session.requestRouteRefresh(route, 0, routesRefreshRequestCallback)
        )
    }

    @Test
    fun `route refresh response - failure`() {
        val error: RouteRefreshError = mockk()
        session.requestRouteRefresh(route, 0, routesRefreshRequestCallback)
        refreshCallback.onError(error)

        verify(exactly = 1) {
            routesRefreshRequestCallback.onError(error)
        }
    }

    @Test
    fun getRouteOptions() {
        session.routes = routes
        assertEquals(routeOptions, session.getPrimaryRouteOptions())
    }

    @Test
    fun cancelAll() {
        session.cancelAll()
        verify { router.cancelAll() }
    }

    @Test
    fun cancelRouteRequest() {
        session.cancelRouteRequest(1L)
        verify { router.cancelRouteRequest(1L) }
    }

    @Test
    fun cancelRouteRefresh() {
        session.cancelRouteRefreshRequest(1L)
        verify { router.cancelRouteRefreshRequest(1L) }
    }

    @Test
    fun shutDown() {
        session.shutdown()
        verify { router.shutdown() }
    }

    @Test
    fun `when route set, observer notified`() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    @Test
    fun `when route cleared, observer notified`() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        session.routes = emptyList()
        verify(exactly = 1) { observer.onRoutesChanged(emptyList()) }
    }

    @Test
    fun `when new route available, observer notified`() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        val newRoutes: List<DirectionsRoute> = listOf(mockk())
        every { newRoutes[0].routeOptions() } returns routeOptions
        session.routes = newRoutes
        verify(exactly = 1) { observer.onRoutesChanged(newRoutes) }
    }

    @Test
    fun `setting a route does not impact ongoing route request`() {
        session.requestRoutes(routeOptions, routerCallback)
        session.routes = routes
        verify(exactly = 0) { router.cancelAll() }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerRoutesObserver(observer)
        session.unregisterAllRoutesObservers()
        session.routes = routes

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }
}
