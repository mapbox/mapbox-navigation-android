package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router
import java.util.concurrent.CopyOnWriteArrayList

// todo make internal
class MapboxDirectionsSession(
    private val router: Router
) : DirectionsSession {

    private val routesObservers = CopyOnWriteArrayList<RoutesObserver>()
    private var routeOptions: RouteOptions? = null

    override var routes: List<DirectionsRoute> = emptyList()
        set(value) {
            router.cancel()
            if (routes.isEmpty() && value.isEmpty()) {
                return
            }
            field = value
            if (routes.isNotEmpty()) {
                this.routeOptions = routes[0].routeOptions()
            }
            routesObservers.forEach { it.onRoutesChanged(value) }
        }

    override fun getRouteOptions(): RouteOptions? = routeOptions

    override fun cancel() {
        router.cancel()
    }

    override fun requestRouteRefresh(route: DirectionsRoute, legIndex: Int, callback: RouteRefreshCallback) {
        router.getRouteRefresh(route, legIndex, callback)
    }

    override fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback?
    ) {
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                this@MapboxDirectionsSession.routes = routes
                routesRequestCallback?.onRoutesReady(routes)
                // todo log in the future
            }

            override fun onFailure(throwable: Throwable) {
                routesRequestCallback?.onRoutesRequestFailure(throwable, routeOptions)
                // todo log in the future
            }

            override fun onCanceled() {
                routesRequestCallback?.onRoutesRequestCanceled(routeOptions)
                // todo log in the future
            }
        })
    }

    override fun requestFasterRoute(
        adjustedRouteOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback
    ) {
        // TODO What happens if NavigationSession.State transitions from Active Guidance
        //  to Free Drive and Faster Route is enabled? We're not cleaning up RouteOptions when
        //  cleaning up routes here and this call can be done because the only thing that we're
        //  checking is the route options and the route progress but not if there's a route or not
        //  See AdjustedRouteOptionsProvider#getRouteOptions
        if (routes.isEmpty()) {
            routesRequestCallback.onRoutesRequestCanceled(adjustedRouteOptions)
            return
        }
        router.getRoute(adjustedRouteOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                routesRequestCallback.onRoutesReady(routes)
            }

            override fun onFailure(throwable: Throwable) {
                ifNonNull(routeOptions) { options ->
                    routesRequestCallback.onRoutesRequestFailure(throwable, options)
                }
            }

            override fun onCanceled() {
                ifNonNull(routeOptions) { options ->
                    routesRequestCallback.onRoutesRequestCanceled(options)
                }
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
    }
}
