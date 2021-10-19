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
import com.mapbox.navigation.core.internal.utils.RoutesUpdateReasonHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val reasonHelper: RoutesUpdateReasonHelper = mockk()
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
    private val mockReason = RoutesExtra.ROUTES_UPDATE_REASON_NEW

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
        every { reasonHelper.getReason(any(), any()) } returns mockReason
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
        session.setRoutes(routes, 0, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(routeOptions, session.getPrimaryRouteOptions())
    }

    @Test
    fun getInitialLegIndex() {
        val initialLegIndex = 2
        session.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(initialLegIndex, session.initialLegIndex)
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
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, 0, mockReason)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, mockReason)
        assertEquals(slot.captured.routes, routes)
    }

    @Test
    fun `observer notified on subscribe with actual route data`() {
        session.setRoutes(routes, 0, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(slot.captured.routes, routes)
    }

    @Test
    fun `when route cleared, observer notified`() {
        val slot = mutableListOf<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, 0, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        session.setRoutes(emptyList(), 0, RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP)

        assertTrue(slot.size == 2)
        assertEquals(slot[0].reason, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(slot[0].routes, routes)
        assertEquals(slot[1].reason, RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP)
        assertEquals(slot[1].routes, emptyList<DirectionsRoute>())
    }

    @Test
    fun `when new route available, observer notified`() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, 0, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        val newRoutes: List<DirectionsRoute> = listOf(mockk())
        every { newRoutes[0].routeOptions() } returns routeOptions
        session.setRoutes(newRoutes, 0, RoutesExtra.ROUTES_UPDATE_REASON_NEW)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(slot.captured.routes, newRoutes)
    }

    @Test
    fun `setting a route does not impact ongoing route request`() {
        session.requestRoutes(routeOptions, routerCallback)
        session.setRoutes(routes, 0, mockReason)
        verify(exactly = 0) { router.cancelAll() }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerRoutesObserver(observer)
        session.unregisterAllRoutesObservers()
        session.setRoutes(routes, 0, mockReason)

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }
}
