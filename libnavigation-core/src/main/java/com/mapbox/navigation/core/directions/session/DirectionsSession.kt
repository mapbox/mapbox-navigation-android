package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.internal.utils.mapToReason

internal interface DirectionsSession : RouteRefresh {

    val routesUpdatedResult: RoutesUpdatedResult?
    val routes: List<NavigationRoute>

    val initialLegIndex: Int

    fun setRoutes(routes: DirectionsSessionRoutes)

    /**
     * Provide route options for current primary route.
     */
    fun getPrimaryRouteOptions(): RouteOptions?

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param routerCallback Callback that gets notified with the results of the request
     *
     * @return requestID, see [cancelRouteRequest]
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        routerCallback: NavigationRouterCallback
    ): Long

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    fun cancelRouteRequest(requestId: Long)

    /**
     * Interrupts all requests if any are in progress.
     */
    fun cancelAll()

    /**
     * Registers [RoutesObserver]. Updated on each change of [routes]
     */
    fun registerRoutesObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters [RoutesObserver]
     */
    fun unregisterRoutesObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters all [RoutesObserver]
     */
    fun unregisterAllRoutesObservers()

    /**
     * Interrupts the route-fetching request
     */
    fun shutdown()
}

internal data class DirectionsSessionRoutes(
    val acceptedRoutes: List<NavigationRoute>,
    val ignoredRoutes: List<IgnoredRoute>,
    val setRoutesInfo: SetRoutes,
) {

    fun toRoutesUpdatedResult(): RoutesUpdatedResult = RoutesUpdatedResult(
        acceptedRoutes,
        ignoredRoutes,
        setRoutesInfo.mapToReason()
    )
}
