package com.mapbox.navigation

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

class DefaultDirectionsSessionTest {

    private lateinit var session: DefaultDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val origin: Point = mockk(relaxUnitFun = true)
    private val destination: Point = mockk(relaxUnitFun = true)
    private val waypoints: List<Point> = mockk(relaxUnitFun = true)
    private val observer: DirectionsSession.RouteObserver = mockk(relaxUnitFun = true)
    private lateinit var routeListener: Router.RouteListener
    private val route: Route = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        val listener = slot<Router.RouteListener>()
        every { router.getRoute(origin, waypoints, destination, capture(listener)) } answers {
            routeListener = listener.captured
        }
        session = DefaultDirectionsSession(router, origin, waypoints, destination)
    }

    @Test
    fun initialRouteResponse() {
        assertNull(session.currentRoute)

        routeListener.onRouteReady(route)

        assertEquals(route, session.currentRoute)
    }

    @Test
    fun setCurrentRoute() {
        val newRoute: Route = mockk()

        session.currentRoute = newRoute

        assertEquals(newRoute, session.currentRoute)
    }

    @Test
    fun getOrigin() {
        assertEquals(origin, session.origin)
    }

    @Test
    fun setOrigin() {
        val newOrigin: Point = mockk()
        val newDestination: Point = mockk()

        session.origin = newOrigin
        session.destination = newDestination

        assertNull(session.currentRoute)
        assertEquals(newOrigin, session.origin)
        verify { router.getRoute(eq(newOrigin), eq(waypoints), eq(newDestination), any()) }
    }

    @Test
    fun getWaypoints() {
        assertEquals(waypoints, session.waypoints)
    }

    @Test
    fun setWaypoints() {
        val newWaypoints: List<Point> = mockk()

        session.waypoints = newWaypoints

        assertNull(session.currentRoute)
        assertEquals(newWaypoints, session.waypoints)
        verify { router.getRoute(eq(origin), eq(newWaypoints), eq(destination), any()) }
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
        session.registerRouteObserver(observer)
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
