package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.utils.timer.MapboxTimer
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
    private val observer: RouteObserver = mockk(relaxUnitFun = true)
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
        session = MapboxDirectionsSession(router)
    }

    @Test
    fun initialState() {
        assertNull(session.getRouteOptions())
        assertEquals(session.getRoutes(), emptyList<DirectionsRoute>())
    }

    @Test
    fun routeResponse_before() {
        session.registerRouteObserver(observer)
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)

        assertEquals(routes, session.getRoutes())
        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun routeResponse_inProgress() {
        session.requestRoutes(routeOptions)

        session.registerRouteObserver(observer)
        verify { observer.onRoutesRequested() }
        verify(exactly = 0) { observer.onRoutesChanged(routes) }

        callback.onResponse(routes)
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun routeResponse_after() {
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)
        session.registerRouteObserver(observer)
        verify(exactly = 0) { observer.onRoutesRequested() }
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun failRouteResponse_before() {
        val throwable: Throwable = mockk()
        session.registerRouteObserver(observer)
        session.requestRoutes(routeOptions)
        verify { observer.onRoutesRequested() }
        callback.onFailure(throwable)
        verify { observer.onRoutesRequestFailure(throwable) }
    }

    @Test
    fun failRouteResponse_inProgress() {
        val throwable: Throwable = mockk()
        session.requestRoutes(routeOptions)
        session.registerRouteObserver(observer)
        verify { observer.onRoutesRequested() }
        callback.onFailure(throwable)
        verify { observer.onRoutesRequestFailure(throwable) }
    }

    @Test
    fun failRouteResponse_after() {
        val throwable: Throwable = mockk()
        session.requestRoutes(routeOptions)
        callback.onFailure(throwable)
        session.registerRouteObserver(observer)
        verify(exactly = 0) { observer.onRoutesRequested() }
        verify { observer.onRoutesRequestFailure(throwable) }
    }

    @Test
    fun getRouteOptions() {
        val routeOptions: RouteOptions = mockk()
        session.requestRoutes(routeOptions)

        assertEquals(routeOptions, session.getRouteOptions())
    }

    @Test
    fun cancel() {
        session.cancel()
        verify { router.cancel() }
    }

    @Test
    fun fasterRoute_timerStartedOnce() {
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)
        session.requestRoutes(routeOptions)
        verify(exactly = 1) { mapboxTimer.start() }
    }

    @Test
    fun fasterRoute_timerShutdown() {
        session.shutDownSession()
        verify { mapboxTimer.stop() }
    }

    @Test
    fun fasterRoute_hasRoute() {
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)
        delayLambda()
        verify { router.getRoute(routeOptions, callback) }
    }

    @Test
    fun fasterRoute_noRoutes() {
        delayLambda()
        verify(exactly = 0) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_failedRoutes() {
        session.requestRoutes(routeOptions)
        val throwable: Throwable = mockk()
        callback.onFailure(throwable)
        delayLambda()
        verify(exactly = 1) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_inProgress() {
        session.requestRoutes(routeOptions)
        delayLambda()
        verify(exactly = 1) { router.getRoute(any(), any()) }
    }

    @Test
    fun fasterRoute_canceled() {
        session.requestRoutes(routeOptions)
        verify(exactly = 1) { router.cancel() }
        callback.onResponse(routes)

        delayLambda()
        session.cancel()
        verify(exactly = 2) { router.cancel() }
    }

    @Test
    fun fasterRoute_canceledByNewRequest() {
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)
        delayLambda()
        session.requestRoutes(routeOptions)
        verify(exactly = 2) { router.cancel() }
    }
}
