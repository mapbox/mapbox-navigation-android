package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.extensions.supportsRouteRefresh
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.MapboxTimer

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 *
 * If the route is successfully refreshed, this class will update the [TripSession.route]
 *
 * [attach] and [stop] are attached to the application lifecycle. Observing routes that
 * can be refreshed are handled by this class. Calling [attach] will restart the refresh timer.
 */
internal class RouteRefreshController(
    private val routeRefreshOptions: RouteRefreshOptions,
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val logger: Logger
) {

    companion object {
        internal val TAG = Tag("MbxRouteRefreshController")
    }

    private val routerRefreshTimer = MapboxTimer().apply {
        restartAfterMillis = routeRefreshOptions.intervalMillis
    }

    private var currentRequestId: Long? = null

    /**
     * If route refresh is enabled, attach a refresh poller.
     * Cancel old timers, and cancel pending requests.
     */
    fun restart() {
        stop()
        if (routeRefreshOptions.enabled) {
            routerRefreshTimer.startTimer {
                refreshRoute()
            }
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

    private fun refreshRoute() {
        val route = tripSession.route
            ?.takeIf { it.routeOptions().supportsRouteRefresh() }
            ?.takeIf { it.isUuidValidForRefresh() }
        if (route != null) {
            val legIndex = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0
            currentRequestId?.let { directionsSession.cancelRouteRefreshRequest(it) }
            currentRequestId = directionsSession.requestRouteRefresh(
                route,
                legIndex,
                routeRefreshCallback
            )
        } else {
            logger.w(
                TAG,
                Message(
                    """
                        The route is not qualified for route refresh feature.
                        See com.mapbox.navigation.base.extensions.supportsRouteRefresh
                        extension for details.
                        ${route?.routeOptions()}
                    """.trimIndent()
                )
            )
        }
    }

    private val routeRefreshCallback = object : RouteRefreshCallback {

        override fun onRefresh(directionsRoute: DirectionsRoute) {
            logger.i(TAG, msg = Message("Successful route refresh"))
            val directionsSessionRoutes = directionsSession.routes.toMutableList()
            if (directionsSessionRoutes.isNotEmpty()) {
                directionsSessionRoutes[0] = directionsRoute
                directionsSession.routes = directionsSessionRoutes
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
