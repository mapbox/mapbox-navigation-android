package com.mapbox.navigation.directions.session

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
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
    private val origin: Point = mockk(relaxUnitFun = true)
    private val destination: Point = mockk(relaxUnitFun = true)
    private val waypoints: List<Point> = mockk(relaxUnitFun = true)
    private lateinit var observer: DirectionsSession.RouteObserver
    private lateinit var callback: Router.Callback
    private val routes: List<Route> = listOf(mockk())

    @Before
    fun setUp() {
        val listener = slot<Router.Callback>()
        observer = mockk(relaxUnitFun = true)
        every { router.getRoute(origin, waypoints, destination, capture(listener)) } answers {
            callback = listener.captured
        }
        session = MapboxDirectionsSession(router, observer)
    }

    @Test
    fun initialState() {
        assertNull(session.getDestination())
        assertNull(session.getOrigin())
        assertEquals(session.getWaypoints(), emptyList<Point>())
        assertEquals(session.getRoutes(), emptyList<Route>())
    }

    @Test
    fun routeResponse() {
        session.requestRoutes(origin, waypoints, destination)
        callback.onResponse(routes)

        assertEquals(routes, session.getRoutes())
        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun failRouteResponse() {
        session.requestRoutes(origin, waypoints, destination)
        callback.onFailure(mockk())

        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesRequestFailure(any()) }
    }

    @Test
    fun getOrigin() {
        val origin: Point = mockk()
        session.requestRoutes(origin, waypoints, destination)

        assertEquals(origin, session.getOrigin())
    }

    @Test
    fun getWaypoints() {
        val waypoints: List<Point> = mockk()
        session.requestRoutes(origin, waypoints, destination)

        assertEquals(waypoints, session.getWaypoints())
    }

    @Test
    fun getDestination() {
        val destination: Point = mockk()
        session.requestRoutes(origin, waypoints, destination)

        assertEquals(destination, session.getDestination())
    }

    @Test
    fun cancel() {
        session.cancel()

        verify { router.cancel() }
    }
}
