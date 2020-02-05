package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.utils.timer.MapboxTimer
import io.mockk.clearMocks
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
    private val routesRequestCallback: RoutesRequestCallback = mockk(relaxUnitFun = true)
    private val observer: RoutesObserver = mockk(relaxUnitFun = true)
    private lateinit var callback: Router.Callback
    private val routes: List<DirectionsRoute> = listOf(mockk())
    private val mapboxTimer: MapboxTimer = mockk(relaxUnitFun = true)
    private lateinit var delayLambda: () -> Unit

    @Before
    fun setUp() {
        val listener = slot<Router.Callback>()
        every { router.getRoute(routeOptions, capture(listener)) } answers {
            callback = listener.captured
        }
        val lambda = slot<() -> Unit>()
        mockkObject(NavigationComponentProvider)
        every { NavigationComponentProvider.createMapboxTimer(120000L, capture(lambda)) } answers {
            delayLambda = lambda.captured
            mapboxTimer
        }
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
        verify(exactly = 1) { routesRequestCallback.onRoutesRequestFailure(throwable, routeOptions) }
        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun getRouteOptions() {
        val routeOptions: RouteOptions = mockk()
        session.requestRoutes(routeOptions, routesRequestCallback)

        assertEquals(routeOptions, session.getRouteOptions())
    }

    @Test
    fun cancel() {
        session.cancel()
        verify { router.cancel() }
    }

    @Test
    fun fasterRoute_timerStartedOnce() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        session.requestRoutes(routeOptions, routesRequestCallback)
        verify(exactly = 1) { mapboxTimer.start() }
    }

    @Test
    fun fasterRoute_timerShutdown() {
        session.shutDownSession()
        verify { mapboxTimer.stop() }
    }

    @Test
    fun fasterRoute_hasRoute() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        delayLambda()
        verify(exactly = 1) { router.getRoute(routeOptions, callback) }
    }

    @Test
    fun fasterRoute_noRoutes() {
        delayLambda()
        verify(exactly = 0) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_failedRoutes() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        val throwable: Throwable = mockk()
        callback.onFailure(throwable)
        delayLambda()
        verify(exactly = 1) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_inProgress() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        delayLambda()
        verify(exactly = 1) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_canceled() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        verify(exactly = 1) { router.cancel() }
        callback.onResponse(routes)

        clearMocks(router)
        delayLambda()
        session.cancel()
        verify(exactly = 1) { router.cancel() }
    }

    @Test
    fun fasterRoute_canceledByNewRequest() {
        session.requestRoutes(routeOptions, routesRequestCallback)
        callback.onResponse(routes)
        delayLambda()
        clearMocks(router)
        session.requestRoutes(routeOptions, routesRequestCallback)
        verify(exactly = 1) { router.cancel() }
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

    @Test
    fun routeRequestClearsSession() {
        session.registerRoutesObserver(observer)
        session.routes = routes
        session.requestRoutes(routeOptions, routesRequestCallback)
        verify(exactly = 1) { observer.onRoutesChanged(emptyList()) }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerRoutesObserver(observer)
        session.requestRoutes(routeOptions, routesRequestCallback)

        session.unregisterAllRoutesObservers()

        callback.onResponse(routes)
        delayLambda()

        verify { router.getRoute(routeOptions, callback) }
        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }
}
