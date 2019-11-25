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
        session = MapboxDirectionsSession(router, origin, waypoints, destination, observer)
    }

    @Test
    fun routeResponse() {
        session.requestRoutes()
        callback.onResponse(routes)

        assertEquals(routes, session.getRoutes())
        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesChanged(routes) }
    }

    @Test
    fun failRouteResponse() {
        session.requestRoutes()
        callback.onFailure(mockk())

        verify { observer.onRoutesRequested() }
        verify { observer.onRoutesRequestFailure(any()) }
    }

    @Test
    fun getOrigin() {
        assertEquals(origin, session.getOrigin())
    }

    @Test
    fun setOrigin() {
        val newOrigin: Point = mockk()
        session.setOrigin(newOrigin)

        assertEquals(newOrigin, session.getOrigin())
        verify { router.getRoute(eq(newOrigin), eq(waypoints), any(), any()) }
    }

    @Test
    fun getWaypoints() {
        assertEquals(waypoints, session.getWaypoints())
    }

    @Test
    fun setWaypoints() {
        val newWaypoints: List<Point> = mockk()
        session.setWaypoints(newWaypoints)

        assertEquals(newWaypoints, session.getWaypoints())
        verify { router.getRoute(eq(origin), eq(newWaypoints), any(), any()) }
    }

    @Test
    fun getDestination() {
        assertEquals(destination, session.getDestination())
    }

    @Test
    fun setDestination() {
        val newDestination: Point = mockk()
        session.setDestination(newDestination)

        assertEquals(newDestination, session.getDestination())
        verify { router.getRoute(any(), any(), eq(newDestination), any()) }
    }

    @Test
    fun cancel() {
        session.cancel()

        verify { router.cancel() }
    }
}
