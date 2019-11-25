package com.mapbox.navigation.directions.session

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route

@MapboxNavigationModule(MapboxNavigationModuleType.DirectionsSession, skipConfiguration = true)
class MapboxDirectionsSession(
    private val router: Router,
    private var origin: Point,
    private var waypoints: List<Point> = emptyList(),
    private var destination: Point,
    private val routeObserver: DirectionsSession.RouteObserver
) : DirectionsSession {

    private var currentRoutes: List<Route> = emptyList()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            routeObserver.onRoutesChanged(value)
        }

    override fun getRoutes() = currentRoutes

    override fun setOrigin(point: Point) {
        origin = point
    }

    override fun getOrigin() = origin

    override fun setWaypoints(points: List<Point>) {
        waypoints = points
    }

    override fun getWaypoints() = waypoints

    override fun setDestination(point: Point) {
        destination = point
    }

    override fun getDestination(): Point = destination

    override fun cancel() {
        router.cancel()
    }

    override fun requestRoutes() {
        router.cancel()
        currentRoutes = emptyList()
        routeObserver.onRoutesRequested()
        router.getRoute(origin, waypoints, destination, object : Router.Callback {
            override fun onResponse(routes: List<Route>) {
                currentRoutes = routes
            }

            override fun onFailure(throwable: Throwable) {
                routeObserver.onRoutesRequestFailure(throwable)
            }
        })
    }
}
