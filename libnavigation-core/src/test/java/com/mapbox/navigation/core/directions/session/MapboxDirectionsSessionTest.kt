package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.NavigationComponentProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptions = mockk(relaxUnitFun = true)
    private val routesRequestCallback: RoutesRequestCallback = mockk(relaxUnitFun = true)
    private val observer: RoutesObserver = mockk(relaxUnitFun = true)
    private val route: DirectionsRoute = mockk(relaxUnitFun = true)
    private val routes: List<DirectionsRoute> = listOf(route)
    private lateinit var callback: Router.Callback

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

        val listener = slot<Router.Callback>()
        every { router.getRoute(routeOptions, capture(listener)) } answers {
            callback = listener.captured
        }
        every { routes[0].routeOptions() } returns routeOptions
        mockkObject(NavigationComponentProvider)
        every { routesRequestCallback.onRoutesReady(any()) } answers {
            this.value
        }
        session = MapboxDirectionsSession(router)
    }

    @Test
    fun initialState() {
        assertNull(session.getRouteOptions())
        assertEquals(session.routes, emptyList<DirectionsRoute>())
    }

    @Test
    fun routeResponse_before() {
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)

        assertEquals(routes, session.routes)
        session.routes.forEach { route ->
            assertEquals(route.routeOptions(), routeOptions)
        }
        verify(exactly = 1) { routesRequestCallback.onRoutesReady(routes) }
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    @Test
    fun routeResponse_inProgress() {
        session.requestRoutes(routeOptions, routesRequestCallback)

        session.registerRoutesObserver(observer)
        verify(exactly = 0) { observer.onRoutesChanged(routes) }

        callback.onResponse(routes)
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    @Test
    fun routeResponse_after() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        session.registerRoutesObserver(observer)
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    @Test
    fun failRouteResponse() {
        val throwable: Throwable = mockk()
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onFailure(throwable)
        verify(exactly = 1) {
            routesRequestCallback.onRoutesRequestFailure(throwable, routeOptions)
        }
        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun getRouteOptions() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        assertEquals(routeOptions, session.getRouteOptions())
    }

    @Test
    fun cancel() {
        session.cancel()
        verify { router.cancel() }
    }

    @Test
    fun shutDown() {
        session.shutdown()
        verify { router.shutdown() }
    }

    @Test
    fun routeSetter_set() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    @Test
    fun routeSetter_setEmpty() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        session.routes = emptyList()
        verify(exactly = 1) { observer.onRoutesChanged(emptyList()) }
    }

    @Test
    fun routeSetter_set_alreadyAvailable() {
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        val newRoutes: List<DirectionsRoute> = listOf(mockk())
        every { newRoutes[0].routeOptions() } returns routeOptions
        session.routes = newRoutes
        verify(exactly = 1) { observer.onRoutesChanged(newRoutes) }
    }

    @Test
    fun routeSetter_cancelRouter() {
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)
        clearMocks(router)
        session.routes = routes
        verify(exactly = 1) { router.cancel() }
        verify(exactly = 1) { observer.onRoutesChanged(routes) }
    }

    // TODO Should we support the use case being tested here? If so, rewrite this test.
    @Ignore
    @Test
    fun routeRequestClearsSession() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        session.requestRoutes(routeOptions, routesRequestCallback)
        verify(exactly = 1) { observer.onRoutesChanged(emptyList()) }
    }

    @Test
    fun fasterRoute_availableRoutes() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        session.requestFasterRoute(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        verify { routesRequestCallback.onRoutesReady(routes) }
    }

    @Test
    fun fasterRoute_failedRoutes() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        val throwable: Throwable = mockk()
        session.requestFasterRoute(routeOptions, routesRequestCallback)
        callback.onFailure(throwable)
        verify { routesRequestCallback.onRoutesRequestFailure(any(), any()) }
    }

    @Test
    fun fasterRoute_canceledRoutes() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        session.requestFasterRoute(routeOptions, routesRequestCallback)
        callback.onCanceled()
        verify { routesRequestCallback.onRoutesRequestCanceled(any()) }
    }

    // TODO Should we support the use case being tested here? If so, rewrite this test.
    @Ignore
    @Test
    fun fasterRoute_canceledByNewRequest() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        session.requestFasterRoute(routeOptions, routesRequestCallback)
        clearMocks(router)
        session.requestRoutes(routeOptions, routesRequestCallback)
        session.requestFasterRoute(routeOptions, routesRequestCallback)
        verify(exactly = 1) { router.cancel() }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)

        session.unregisterAllRoutesObservers()

        callback.onResponse(routes)

        verify { router.getRoute(routeOptions, callback) }
        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }
}
