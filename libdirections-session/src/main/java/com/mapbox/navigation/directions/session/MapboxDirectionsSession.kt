package com.mapbox.navigation.directions.session

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import java.util.concurrent.CopyOnWriteArrayList

@MapboxNavigationModule(MapboxNavigationModuleType.DirectionsSession, skipConfiguration = true)
class MapboxDirectionsSession(
    private val router: Router,
    private var origin: Point,
    private var waypoints: List<Point>,
    private var destination: Point,
    routeObserver: DirectionsSession.RouteObserver?
) : DirectionsSession {

    private val routeObservers = CopyOnWriteArrayList<DirectionsSession.RouteObserver>()

    init {
        routeObserver?.let { registerRouteObserver(it) }
        requestRoute()
    }

    override var currentRoute: Route? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            routeObservers.forEach { it.onRouteChanged(value) }
        }

    override fun setOrigin(point: Point) {
        origin = point
        requestRoute()
    }

    override fun getOrigin() = origin

    override fun setWaypoints(points: List<Point>) {
        waypoints = points
        requestRoute()
    }

    override fun getWaypoints() = waypoints

    override fun setDestination(point: Point) {
        destination = point
        requestRoute()
    }

    override fun getDestination(): Point = destination

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
        router.cancel()
        router.getRoute(origin, waypoints, destination, object : Router.Callback {
            override fun onRouteReady(routes: List<Route>) {
                val route = routes.firstOrNull()
                currentRoute = route
                routeObservers.forEach { it.onRouteChanged(route) }
            }

            override fun onFailure(throwable: Throwable) {
                currentRoute = null
                routeObservers.forEach { it.onFailure(throwable) }
            }
        })
    }
}
