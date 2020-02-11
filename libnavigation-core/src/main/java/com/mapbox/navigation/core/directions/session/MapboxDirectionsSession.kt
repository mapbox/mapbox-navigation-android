package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.ifNonNull
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

    override fun requestFasterRoute(
        adjustedRouteOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback
    ) {
        if (routes.isEmpty()) {
            routesRequestCallback.onRoutesRequestCanceled(adjustedRouteOptions)
            return
        }
        this.routeOptions = adjustedRouteOptions
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
