package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [DirectionsSession].
 *
 * @property router route fetcher. Usually Onboard, Offboard or Hybrid
 * @property routes a list of [DirectionsRoute]. Fetched from [Router] or might be set manually
 */
internal class MapboxDirectionsSession(
    private val router: Router
) : DirectionsSession {

    private val routesObservers = CopyOnWriteArraySet<RoutesObserver>()
    private var primaryRouteOptions: RouteOptions? = null

    /**
     * Routes that were fetched from [Router] or set manually.
     * On [routes] change notify registered [RoutesObserver]
     *
     * @see [registerRoutesObserver]
     */
    override var routes: List<DirectionsRoute> = emptyList()
        set(value) {
            if (routes.isEmpty() && value.isEmpty()) {
                return
            }
            field = value
            if (routes.isNotEmpty()) {
                this.primaryRouteOptions = routes[0].routeOptions()
            }
            routesObservers.forEach { it.onRoutesChanged(value) }
        }

    /**
     * Provide route options for current primary route.
     */
    override fun getPrimaryRouteOptions(): RouteOptions? = primaryRouteOptions

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    override fun cancelAll() {
        router.cancelAll()
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    override fun requestRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long {
        return router.getRouteRefresh(route, legIndex, callback)
    }

    /**
     * Cancels [requestRouteRefresh].
     */
    override fun cancelRouteRefreshRequest(requestId: Long) {
        router.cancelRouteRefreshRequest(requestId)
    }

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param routerCallback Callback that gets notified with the results of the request(optional),
     * see [registerRoutesObserver]
     *
     * @return requestID, see [cancelRouteRequest]
     */
    override fun requestRoutes(
        routeOptions: RouteOptions,
        routerCallback: RouterCallback
    ): Long {
        return router.getRoute(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    routerCallback.onRoutesReady(routes, routerOrigin)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    routerCallback.onFailure(reasons, routeOptions)
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    routerCallback.onCanceled(routeOptions, routerOrigin)
                }
            }
        )
    }

    override fun cancelRouteRequest(requestId: Long) {
        router.cancelRouteRequest(requestId)
    }

    /**
     * Registers [RoutesObserver]. Updated on each change of [routes]
     */
    override fun registerRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.add(routesObserver)
        if (routes.isNotEmpty()) {
            routesObserver.onRoutesChanged(routes)
        }
    }

    /**
     * Unregisters [RoutesObserver]
     */
    override fun unregisterRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.remove(routesObserver)
    }

    /**
     * Unregisters all [RoutesObserver]
     */
    override fun unregisterAllRoutesObservers() {
        routesObservers.clear()
    }

    /**
     * Interrupt route-fetcher request
     */
    override fun shutdown() {
        router.shutdown()
    }
}
