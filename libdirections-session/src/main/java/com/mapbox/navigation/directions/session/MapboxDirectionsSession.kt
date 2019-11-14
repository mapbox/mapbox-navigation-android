package com.mapbox.navigation.directions.session

import android.location.Location
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import java.util.concurrent.CopyOnWriteArrayList

@MapboxNavigationModule(MapboxNavigationModuleType.DirectionsSession, skipConfiguration = true)
class MapboxDirectionsSession(
    private val router: Router,
    origin: Location,
    waypoints: List<Location>,
    private val destination: Location
) : DirectionsSession {

    init {
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

    private var origin: Location = origin

    private var waypoints: List<Location> = waypoints

    private val routeObservers = CopyOnWriteArrayList<DirectionsSession.RouteObserver>()

    override fun setOrigin(point: Location) {
        origin = point
        requestRoute()
    }

    override fun getOrigin() = origin

    override fun setWaypoints(points: List<Location>) {
        waypoints = points
        requestRoute()
    }

    override fun getWaypoints() = waypoints

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
        currentRoute = null
        router.getRoute(origin, waypoints, destination, object : Router.RouteCallback {
            override fun onRouteReady(routes: List<Route>) {
                val route = routes.firstOrNull()
                currentRoute = route
                routeObservers.forEach { it.onRouteChanged(route) }
            }

            override fun onFailure(throwable: Throwable) {
                routeObservers.forEach { it.onFailure(throwable) }
            }
        })
    }
}
