package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.NavigationComponentProvider
import java.util.concurrent.CopyOnWriteArrayList

// todo make internal
class MapboxDirectionsSession(
    private val router: Router
) : DirectionsSession {

    private val fasterRouteInterval = 2 * 60 * 1000L // 2 minutes
    private val routesObservers = CopyOnWriteArrayList<RoutesObserver>()
    private var routeOptions: RouteOptions? = null
    private val fasterRouteTimer =
        NavigationComponentProvider.createMapboxTimer(fasterRouteInterval) {
            routeOptions?.let { requestFasterRoute(it) }
        }

    override var routes: List<DirectionsRoute> = emptyList()
        set(value) {
            router.cancel()
            if (routes.isEmpty() && value.isEmpty()) {
                return
            }
            field = value
            if (value.isEmpty()) {
                fasterRouteTimer.stop()
            } else {
                fasterRouteTimer.start()
            }
            routesObservers.forEach { it.onRoutesChanged(value) }
        }

    override fun getRouteOptions(): RouteOptions? = routeOptions

    override fun cancel() {
        router.cancel()
    }

    override fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback
    ) {
        routes = emptyList()
        this.routeOptions = routeOptions
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                this@MapboxDirectionsSession.routes = routesRequestCallback.onRoutesReady(routes)
            }

            override fun onFailure(throwable: Throwable) {
                routesRequestCallback.onRoutesRequestFailure(throwable, routeOptions)
            }

            override fun onCanceled() {
                routesRequestCallback.onRoutesRequestCanceled(routeOptions)
            }
        })
    }

    override fun registerRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.add(routesObserver)
        if (routes.isNotEmpty()) {
            routesObserver.onRoutesChanged(routes)
        }
    }

    override fun unregisterRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.remove(routesObserver)
    }

    override fun unregisterAllRoutesObservers() {
        routesObservers.clear()
    }

    override fun shutDownSession() {
        cancel()
        fasterRouteTimer.stop()
    }

    private fun requestFasterRoute(routeOptions: RouteOptions) {
        if (routes.isEmpty()) {
            return
        }
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                if (isRouteFaster(routes[0])) {
                    this@MapboxDirectionsSession.routes = routes
                }
            }

            override fun onFailure(throwable: Throwable) {
                // do nothing
            }

            override fun onCanceled() {
                // do nothing
            }
        })
    }

    private fun isRouteFaster(newRoute: DirectionsRoute): Boolean {
        // TODO: Implement the logic to check if the route is faster
        return false
    }
}
