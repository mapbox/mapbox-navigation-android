package com.mapbox.navigation.core.directions.session

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.internal.utils.mapToReason
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions

internal interface DirectionsSession : RouteRefresh {

    @VisibleForTesting
    val routesUpdatedResult: RoutesUpdatedResult?

    val routes: List<NavigationRoute>

    /**
     * A list of routes that have been ignored.
     *
     * Ignored routes are those that are not currently being used for navigation,
     * but are still kept in memory for potential future use.
     */
    val ignoredRoutes: List<IgnoredRoute>

    val initialLegIndex: Int

    /**
     * Notify that route setting process has started.
     * During the process of routes update, NavSDK skips all the [NavigationStatus] updates.
     *
     * Make sure to call [setNavigationRoutesFinished] when done to release the updates.
     * */
    fun setNavigationRoutesStarted(params: RoutesSetStartedParams)

    /**
     * Notify that route setting process has finished.
     *
     * @param routes DirectionsSessionRoutes
     */
    fun setNavigationRoutesFinished(routes: DirectionsSessionRoutes)

    /**
     * Provide route options for current primary route.
     */
    fun getPrimaryRouteOptions(): RouteOptions?

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param signature information about what triggered this route request
     * @param routerCallback Callback that gets notified with the results of the request
     *
     * @return requestID, see [cancelRouteRequest]
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        routerCallback: NavigationRouterCallback,
    ): Long

    /**
     * Fetch map-matched routes based on [MapMatchingOptions] via the native Map Matching API.
     * Same flow and cancellation as [requestRoutes].
     *
     * @param mapMatchingOptions map matching options (same as for [MapboxNavigation.requestMapMatching])
     * @param signature information about what triggered this route request
     * @param callback Callback that gets notified with the results of the request
     * @return requestID, see [cancelRouteRequest]
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun requestMapMatchedRoutes(
        mapMatchingOptions: MapMatchingOptions,
        signature: GetRouteSignature,
        callback: MapMatchingAPICallback,
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
    fun registerSetNavigationRoutesFinishedObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters [RoutesObserver]
     */
    fun unregisterSetNavigationRoutesFinishedObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters all [RoutesObserver]
     */
    fun unregisterAllSetNavigationRoutesFinishedObserver()

    fun registerSetNavigationRoutesStartedObserver(observer: SetNavigationRoutesStartedObserver)

    fun unregisterSetNavigationRoutesStartedObserver(observer: SetNavigationRoutesStartedObserver)

    /**
     * Interrupts the route-fetching request
     */
    fun shutdown()
}

internal val DirectionsSession.routesPlusIgnored: List<NavigationRoute>
    get() = routes + ignoredRoutes.map { it.navigationRoute }

internal fun DirectionsSession.findRoute(routeId: String): NavigationRoute? =
    routesPlusIgnored.find { it.id == routeId }

internal data class DirectionsSessionRoutes(
    val acceptedRoutes: List<NavigationRoute>,
    val ignoredRoutes: List<IgnoredRoute>,
    val setRoutesInfo: SetRoutes,
) {

    fun toRoutesUpdatedResult(): RoutesUpdatedResult = RoutesUpdatedResult(
        acceptedRoutes,
        ignoredRoutes,
        setRoutesInfo.mapToReason(),
    )
}
