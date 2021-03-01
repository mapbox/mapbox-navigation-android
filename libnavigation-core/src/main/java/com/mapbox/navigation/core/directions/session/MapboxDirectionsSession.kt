package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [DirectionsSession].
 *
 * @property router route fetcher. Usually Onboard, Offboard or Hybrid
 * @property routes a list of [DirectionsRoute]. Fetched from [Router] or might be set manually
 */
internal class MapboxDirectionsSession(
    private val router: Router,
    private val logger: Logger
) : DirectionsSession {

    private companion object {
        private const val TAG = "MbxDirectionsSession"
    }

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
     * @param routesRequestCallback Callback that gets notified with the results of the request(optional),
     * see [registerRoutesObserver]
     *
     * @return requestID, see [cancelRouteRequest]
     */
    override fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback
    ): Long {
        return router.getRoute(
            routeOptions,
            object : Router.Callback {
                override fun onResponse(routes: List<DirectionsRoute>) {
                    routesRequestCallback.onRoutesReady(routes)
                }

                override fun onFailure(throwable: Throwable) {
                    logger.e(
                        Tag(TAG),
                        Message(
                            """
                                Route request - failure: $throwable
                                For options: $routeOptions
                            """.trimIndent()
                        )
                    )
                    routesRequestCallback.onRoutesRequestFailure(throwable, routeOptions)
                }

                override fun onCanceled() {
                    logger.d(
                        Tag(TAG),
                        Message(
                            """
                                Route request - canceled.
                                For options: $routeOptions
                            """.trimIndent()
                        )
                    )
                    routesRequestCallback.onRoutesRequestCanceled(routeOptions)
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
