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
    private val routeObserver: DirectionsSession.RouteObserver
) : DirectionsSession {

    private var _origin: Point? = null
    private var _waypoints: List<Point> = emptyList()
    private var _destination: Point? = null

    private var currentRoutes: List<Route> = emptyList()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            routeObserver.onRoutesChanged(value)
        }

    override fun getRoutes() = currentRoutes

    override fun getOrigin(): Point? = _origin

    override fun getWaypoints(): List<Point> = _waypoints

    override fun getDestination(): Point? = _destination

    override fun cancel() {
        router.cancel()
    }

    override fun requestRoutes(origin: Point, waypoints: List<Point>, destination: Point) {
        this._origin = origin
        this._waypoints = waypoints
        this._destination = destination

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
