package com.mapbox.navigation.examples

import android.util.Log
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

class SwitchOnlyRerouteController(
    private val mapboxNavigation: MapboxNavigation
) : NavigationRerouteController {

    private var currentRouteProgressObserver: RouteProgressObserver? = null

    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        interrupt()
        currentRouteProgressObserver = object : RouteProgressObserver {
            override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                val routeAlternativeId = routeProgress.routeAlternativeId
                Log.d("alternative-test-cntr", "${routeProgress.currentState} alternativeId: $routeAlternativeId")
                if (routeAlternativeId != null) {
                    val routes = mapboxNavigation.getNavigationRoutes().toMutableList()
                    val alternativeRoute = routes.firstOrNull { it.id == routeAlternativeId }
                    if (alternativeRoute != null) {
                        routes.remove(alternativeRoute)
                        routes.add(0, alternativeRoute)
                        mapboxNavigation.unregisterRouteProgressObserver(this)
                        callback.onNewRoutes(routes, RouterOrigin.Custom())
                    }
                }
            }
        }.apply {
            mapboxNavigation.registerRouteProgressObserver(this)
        }
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        reroute { routes, _ ->
            routesCallback.onNewRoutes(routes.toDirectionsRoutes())
        }
    }

    override fun interrupt() {
        currentRouteProgressObserver?.let { mapboxNavigation.unregisterRouteProgressObserver(it) }
        currentRouteProgressObserver = null
    }

    override var state: RerouteState = RerouteState.Idle
        private set

    override fun registerRerouteStateObserver(rerouteStateObserver: RerouteController.RerouteStateObserver): Boolean {
        return false
    }

    override fun unregisterRerouteStateObserver(rerouteStateObserver: RerouteController.RerouteStateObserver): Boolean {
        return false
    }
}