package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.MapboxTimer
import com.mapbox.navigation.utils.internal.ThreadController

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 *
 * If the route is successfully refreshed, this class will update the [DirectionsSession].
 */
internal class RouteRefreshController(
    private val routeRefreshOptions: RouteRefreshOptions,
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val logger: Logger,
    threadController: ThreadController,
    private val routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
) {

    internal companion object {
        internal val TAG = Tag("MbxRouteRefreshController")
    }

    private val routerRefreshTimer = MapboxTimer(threadController).apply {
        restartAfterMillis = routeRefreshOptions.intervalMillis
    }

    private var currentRequestId: Long? = null

    /**
     * If route refresh is enabled, attach a refresh poller.
     * Cancel old timers, and cancel pending requests.
     */
    fun restart(route: DirectionsRoute) {
        stop()
        if (route.routeOptions()?.enableRefresh() == true) {
            routerRefreshTimer.startTimer {
                refreshRoute(route)
            }
        } else if (route.routeOptions() == null) {
            logger.w(
                TAG,
                Message(
                    """
                        Unable to refresh the route because routeOptions are missing.
                        Use #fromJson(json, routeOptions, requestUuid)
                        when deserializing the route or route response.
                    """.trimIndent()
                )
            )
        }
    }

    /**
     * Cancel old timers, and cancel pending requests.
     */
    fun stop() {
        routerRefreshTimer.stopJobs()
        currentRequestId?.let {
            directionsSession.cancelRouteRefreshRequest(it)
            currentRequestId = null
        }
    }

    private fun refreshRoute(route: DirectionsRoute) {
        val isValid = route.routeOptions()?.enableRefresh() == true && route.isUuidValidForRefresh()
        if (isValid) {
            val legIndex = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0
            currentRequestId?.let { directionsSession.cancelRouteRefreshRequest(it) }
            currentRequestId = directionsSession.requestRouteRefresh(
                route,
                legIndex,
                createRouteRefreshCallback(route, legIndex),
            )
        } else {
            logger.w(
                TAG,
                Message(
                    """
                        The route is not qualified for route refresh feature.
                        See com.mapbox.navigation.base.extensions.supportsRouteRefresh
                        extension for details.
                        ${route.routeOptions()}
                    """.trimIndent()
                )
            )
        }
    }

    private fun createRouteRefreshCallback(
        oldDirectionsRoute: DirectionsRoute,
        currentLegIndex: Int,
    ) = object : RouteRefreshCallback {

        override fun onRefresh(directionsRoute: DirectionsRoute) {
            logger.i(TAG, msg = Message("Successful route refresh"))
            val routeDiffs = routeDiffProvider.buildRouteDiffs(
                oldDirectionsRoute,
                directionsRoute,
                currentLegIndex,
            )
            if (routeDiffs.isEmpty()) {
                logger.i(TAG, msg = Message("No changes to route annotations"))
            } else {
                for (diff in routeDiffs) {
                    logger.i(TAG, msg = Message(diff))
                }
            }
            val directionsSessionRoutes = directionsSession.routes.toMutableList()
            if (directionsSessionRoutes.isNotEmpty()) {
                directionsSessionRoutes[0] = directionsRoute
                directionsSession.setRoutes(
                    directionsSessionRoutes,
                    routesUpdateReason = RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
                )
            }
            currentRequestId = null
        }

        override fun onError(error: RouteRefreshError) {
            logger.e(
                TAG,
                msg = Message("Route refresh error: ${error.message}"),
                tr = error.throwable
            )
            currentRequestId = null
        }
    }

    /**
     * Check if uuid is valid:
     * - [DirectionsRoute] is not **null**;
     * - uuid is not empty;
     * - uuid is not equal to [MapboxNativeNavigatorImpl.OFFLINE_UUID].
     */
    private fun DirectionsRoute?.isUuidValidForRefresh(): Boolean =
        this?.requestUuid()
            ?.let { uuid -> uuid.isNotEmpty() && uuid != MapboxNativeNavigatorImpl.OFFLINE_UUID }
            ?: false
}
