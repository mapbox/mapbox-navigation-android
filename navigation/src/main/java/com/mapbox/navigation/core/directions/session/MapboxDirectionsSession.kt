package com.mapbox.navigation.core.directions.session

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshCallback
import com.mapbox.navigation.core.internal.router.Router
import com.mapbox.navigation.core.internal.utils.initialLegIndex
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [DirectionsSession].
 *
 * @property router route fetcher. Usually Onboard, Offboard or Hybrid
 * @property routesUpdatedResult info of last routes update.
 */
internal class MapboxDirectionsSession(
    private val router: Router,
) : DirectionsSession {

    private val onSetNavigationRoutesFinishedObservers = CopyOnWriteArraySet<RoutesObserver>()
    private val onSetNavigationRoutesStartedObservers =
        CopyOnWriteArraySet<SetNavigationRoutesStartedObserver>()

    @VisibleForTesting
    override var routesUpdatedResult: RoutesUpdatedResult? = null

    override val routes: List<NavigationRoute>
        get() = routesUpdatedResult?.navigationRoutes ?: emptyList()

    override val ignoredRoutes: List<IgnoredRoute>
        get() = routesUpdatedResult?.ignoredRoutes ?: emptyList()

    override var initialLegIndex = DEFAULT_INITIAL_LEG_INDEX
        private set

    internal companion object {
        internal const val DEFAULT_INITIAL_LEG_INDEX = 0
    }

    override fun setNavigationRoutesFinished(routes: DirectionsSessionRoutes) {
        this.initialLegIndex = routes.setRoutesInfo.initialLegIndex()

        if (
            routesUpdatedResult?.navigationRoutes?.isEmpty() == true &&
            routes.acceptedRoutes.isEmpty()
        ) {
            return
        }

        val result = routes.toRoutesUpdatedResult().also {
            routesUpdatedResult = it
        }

        PerformanceTracker.trackPerformanceSync(
            "MapboxDirectionsSession-dispatch-onRoutesChanged",
        ) {
            onSetNavigationRoutesFinishedObservers.forEach {
                it.onRoutesChanged(result)
            }
        }
    }

    override fun setNavigationRoutesStarted(params: RoutesSetStartedParams) {
        onSetNavigationRoutesStartedObservers.forEach { it.onRoutesSetStarted(params) }
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
    override fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback,
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
     * see [registerSetNavigationRoutesFinishedObserver]
     *
     * @return requestID, see [cancelRouteRequest]
     */
    override fun requestRoutes(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        routerCallback: NavigationRouterCallback,
    ): Long {
        return router.getRoute(routeOptions, signature, routerCallback)
    }

    override fun cancelRouteRequest(requestId: Long) {
        router.cancelRouteRequest(requestId)
    }

    /**
     * Registers [RoutesObserver]. Updated on each change of [routesUpdatedResult]
     */
    override fun registerSetNavigationRoutesFinishedObserver(routesObserver: RoutesObserver) {
        onSetNavigationRoutesFinishedObservers.add(routesObserver)
        routesUpdatedResult?.let { routesObserver.onRoutesChanged(it) }
    }

    /**
     * Unregisters [RoutesObserver]
     */
    override fun unregisterSetNavigationRoutesFinishedObserver(routesObserver: RoutesObserver) {
        onSetNavigationRoutesFinishedObservers.remove(routesObserver)
    }

    /**
     * Unregisters all [RoutesObserver]
     */
    override fun unregisterAllSetNavigationRoutesFinishedObserver() {
        onSetNavigationRoutesFinishedObservers.clear()
    }

    override fun registerSetNavigationRoutesStartedObserver(
        observer: SetNavigationRoutesStartedObserver,
    ) {
        onSetNavigationRoutesStartedObservers.add(observer)
    }

    override fun unregisterSetNavigationRoutesStartedObserver(
        observer: SetNavigationRoutesStartedObserver,
    ) {
        onSetNavigationRoutesStartedObservers.remove(observer)
    }

    /**
     * Interrupt route-fetcher request
     */
    override fun shutdown() {
        router.shutdown()
    }
}
