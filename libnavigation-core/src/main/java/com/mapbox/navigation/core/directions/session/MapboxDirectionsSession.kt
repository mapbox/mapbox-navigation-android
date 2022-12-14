package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.internal.utils.initialLegIndex
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [DirectionsSession].
 *
 * @property router route fetcher. Usually Onboard, Offboard or Hybrid
 * @property routesUpdatedResult info of last routes update. Fetched from [Router] or might be set manually
 */
internal class MapboxDirectionsSession(
    private val router: NavigationRouterV2,
) : DirectionsSession {

    private val routesObservers = CopyOnWriteArraySet<RoutesObserver>()

    override var routesUpdatedResult: RoutesUpdatedResult? = null
    override val routes: List<NavigationRoute>
        get() = routesUpdatedResult?.navigationRoutes ?: emptyList()

    override var initialLegIndex = DEFAULT_INITIAL_LEG_INDEX
        private set

    internal companion object {
        internal const val DEFAULT_INITIAL_LEG_INDEX = 0
    }

    override fun setRoutes(routes: DirectionsSessionRoutes) {
        this.initialLegIndex = routes.setRoutesInfo.initialLegIndex()
        if (
            routesUpdatedResult?.navigationRoutes?.isEmpty() == true &&
            routes.acceptedRoutes.isEmpty()
        ) {
            return
        }
        RouteCompatibilityCache.setDirectionsSessionResult(routes.acceptedRoutes)

        val result = routes.toRoutesUpdatedResult().also { routesUpdatedResult = it }
        routesObservers.forEach {
            it.onRoutesChanged(result)
        }
    }

    /**
     * Provide route options for current primary route.
     */
    override fun getPrimaryRouteOptions(): RouteOptions? =
        routesUpdatedResult?.navigationRoutes?.firstOrNull()?.routeOptions

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
     * @param routeRefreshRequestData Object containing information needed for refresh request
     * @param callback Callback that gets notified with the results of the request
     */
    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback
    ): Long {
        return router.getRouteRefresh(route, routeRefreshRequestData, callback)
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
        routerCallback: NavigationRouterCallback
    ): Long {
        return router.getRoute(routeOptions, routerCallback)
    }

    override fun cancelRouteRequest(requestId: Long) {
        router.cancelRouteRequest(requestId)
    }

    /**
     * Registers [RoutesObserver]. Updated on each change of [routesUpdatedResult]
     */
    override fun registerRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.add(routesObserver)
        routesUpdatedResult?.let { routesObserver.onRoutesChanged(it) }
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
