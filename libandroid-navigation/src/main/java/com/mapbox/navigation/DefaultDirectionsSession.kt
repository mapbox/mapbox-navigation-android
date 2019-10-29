package com.mapbox.navigation

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import java.util.concurrent.CopyOnWriteArrayList

class DefaultDirectionsSession(private val router: Router, origin: Point, waypoints: List<Point>) : DirectionsSession {

    override var currentRoute: Route? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            routeObservers.forEach { it.onRouteChanged(value) }
        }

    override var origin: Point = origin
        set(value) {
            if (field == value) {
                return
            }
            field = value
            requestRoute()
        }

    override var waypoints: List<Point> = waypoints
        set(value) {
            if (field == value) {
                return
            }
            field = value
            requestRoute()
        }

    private val routeObservers = CopyOnWriteArrayList<DirectionsSession.RouteObserver>()

    init {
        requestRoute()
    }

    override fun registerRouteObserver(routeObserver: DirectionsSession.RouteObserver) {
        routeObservers.add(routeObserver)
        routeObserver.onRouteChanged(currentRoute)
    }

    override fun unregisterRouteObserver(routeObserver: DirectionsSession.RouteObserver) {
        routeObservers.remove(routeObserver)
    }

    override fun cancel() {
        router.cancel()
    }

    private fun requestRoute() {
        currentRoute = null
        router.getRoute(origin, waypoints) {
            currentRoute = it
        }
    }
}
