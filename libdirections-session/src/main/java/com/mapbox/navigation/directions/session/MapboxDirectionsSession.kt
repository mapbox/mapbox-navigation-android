package com.mapbox.navigation.directions.session

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation

@MapboxNavigationModule(MapboxNavigationModuleType.DirectionsSession, skipConfiguration = true)
class MapboxDirectionsSession(
    private val router: Router,
    private val routeObserver: DirectionsSession.RouteObserver // TODO investigate inject routeObserver through setter
) : DirectionsSession {

    private var _routeOptions: RouteOptionsNavigation? = null

    private var currentRoutes: List<Route> = emptyList()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            routeObserver.onRoutesChanged(value)
        }

    override fun getRoutes() = currentRoutes

    override fun getRouteOptions(): RouteOptionsNavigation? = _routeOptions

    override fun cancel() {
        router.cancel()
    }

    override fun requestRoutes(routeOptions: RouteOptionsNavigation) {
        this._routeOptions = routeOptions

        router.cancel()
        currentRoutes = emptyList()
        routeObserver.onRoutesRequested()
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<Route>) {
                currentRoutes = routes
            }

            override fun onFailure(throwable: Throwable) {
                routeObserver.onRoutesRequestFailure(throwable)
            }
        })
    }
}
