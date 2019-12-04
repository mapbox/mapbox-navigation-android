package com.mapbox.navigation.directions.session

import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptionsNavigation = mockk(relaxUnitFun = true)
    private lateinit var observer: DirectionsSession.RouteObserver
    private lateinit var callback: Router.Callback
    private val routes: List<Route> = listOf(mockk())

    @Before
    fun setUp() {
        val listener = slot<Router.Callback>()
        observer = mockk(relaxUnitFun = true)
        every { router.getRoute(routeOptions, capture(listener)) } answers {
            callback = listener.captured
        }
        session = MapboxDirectionsSession(router, observer)
    }

    @Test
    fun initialState() {
        assertNull(session.getRouteOptions())
        assertEquals(session.getRoutes(), emptyList<Route>())
    }

    @Test
    fun routeResponse() {
        session.requestRoutes(routeOptions)
        callback.onResponse(routes)

        assertEquals(routes, session.getRoutes())
        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun failRouteResponse() {
        session.requestRoutes(routeOptions)
        callback.onFailure(mockk())

        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesRequestFailure(any()) }
    }

    @Test
    fun getRouteOptions() {
        val routeOptions: RouteOptionsNavigation = mockk()
        session.requestRoutes(routeOptions)

        assertEquals(routeOptions, session.getRouteOptions())
    }

    @Test
    fun cancel() {
        session.cancel()

        verify { router.cancel() }
    }
}
