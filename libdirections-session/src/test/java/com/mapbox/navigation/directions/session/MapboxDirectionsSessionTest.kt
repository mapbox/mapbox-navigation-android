package com.mapbox.navigation.directions.session

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.DirectionsSession
import io.mockk.every
import com.mapbox.navigation.base.route.model.Route
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
    private val observer: DirectionsSession.RouteObserver = mockk(relaxUnitFun = true)
    private val routeCallbackSlot = slot<((route: Route) -> Unit)>()
    private lateinit var routeCallback: Router.RouteCallback
    private val route: Route = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        val listener = slot<Router.RouteCallback>()
        every { router.getRoute(origin, waypoints, destination, capture(listener)) } answers {
            routeCallback = listener.captured
        }
        session = MapboxDirectionsSession(router, origin, waypoints, destination)
    }

    @Test
    fun initialRouteResponse() {
        assertNull(session.currentRoute)
        session.registerRouteObserver(observer)
        session.setWaypoints(waypoints)

        routeCallback.onRouteReady(listOf(route))

        assertEquals(route, session.currentRoute)
        verify { observer.onRouteChanged(route) }
    }

    @Test
    fun failRouteResponse() {
        assertNull(session.currentRoute)
        session.registerRouteObserver(observer)
        session.setWaypoints(waypoints)

        routeCallback.onFailure(mockk())

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
