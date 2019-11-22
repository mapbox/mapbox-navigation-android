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
    private val route: Route = mockk(relaxUnitFun = true)

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
    fun initialRouteResponse() {
        assertNull(session.currentRoute)

        callback.onResponse(listOf(route))

        assertEquals(route, session.currentRoute)
        verify { observer.onRouteChanged(route) }
    }

    @Test
    fun failRouteResponse() {
        assertNull(session.currentRoute)

        callback.onFailure(mockk())

        assertNull(session.currentRoute)
        verify { observer.onFailure(any()) }
    }

    @Test
    fun setCurrentRoute() {
        val newRoute: Route = mockk()

        session.currentRoute = newRoute

        assertEquals(newRoute, session.currentRoute)
    }

    @Test
    fun getOrigin() {
        assertEquals(origin, session.getOrigin())
    }

    @Test
    fun setOrigin() {
        val newOrigin: Point = mockk()
        session.setOrigin(newOrigin)

        assertNull(session.currentRoute)
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

        assertNull(session.currentRoute)
        assertEquals(newWaypoints, session.getWaypoints())
        verify { router.getRoute(eq(origin), eq(newWaypoints), any(), any()) }
    }

    @Test
    fun registerObserver() {
        session.registerRouteObserver(observer)
        verify { observer.onRouteChanged(null) }
        val newRoute: Route = mockk()
        session.currentRoute = newRoute
        verify { observer.onRouteChanged(newRoute) }
    }

    @Test
    fun unregisterObserver() {
        verify { observer.onRouteChanged(null) }
        session.unregisterRouteObserver(observer)
        val newRoute: Route = mockk()
        session.currentRoute = newRoute

        verify(exactly = 0) { observer.onRouteChanged(newRoute) }
    }

    @Test
    fun cancel() {
        session.cancel()

        verify { router.cancel() }
    }
}
