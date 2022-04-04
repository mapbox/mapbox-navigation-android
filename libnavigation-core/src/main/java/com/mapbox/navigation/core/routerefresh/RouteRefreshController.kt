package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.MapboxTimer
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW

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
    threadController: ThreadController,
    private val routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
) {

    internal companion object {
        internal const val LOG_CATEGORY = "RouteRefreshController"
    }

    private val routerRefreshTimer = MapboxTimer(threadController).apply {
        restartAfterMillis = routeRefreshOptions.intervalMillis
    }

    private var currentRequestId: Long? = null

    /**
     * If route refresh is enabled, attach a refresh poller.
     * Cancel old timers, and cancel pending requests.
     */
    fun restart(route: NavigationRoute, onRoutesRefreshed: (List<NavigationRoute>) -> Unit) {
        stop()
        if (route.routeOptions.enableRefresh() == true) {
            routerRefreshTimer.startTimer {
                refreshRoute(route, onRoutesRefreshed)
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

    private fun refreshRoute(route: NavigationRoute, onRoutesRefreshed: (List<NavigationRoute>) -> Unit) {
        val isValid = route.routeOptions.enableRefresh() == true &&
            route.directionsResponse.uuid()?.isNotBlank() == true
        if (isValid) {
            val legIndex = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0
            currentRequestId?.let { directionsSession.cancelRouteRefreshRequest(it) }
            currentRequestId = directionsSession.requestRouteRefresh(
                route,
                legIndex,
                createRouteRefreshCallback(route, legIndex, onRoutesRefreshed),
            )
        } else {
            logW(
                """
                    The route is not qualified for route refresh feature.
                    See com.mapbox.navigation.base.extensions.supportsRouteRefresh
                    extension for details.
                    routeOptions: ${route.routeOptions}
                    uuid: ${route.directionsResponse.uuid()}
                """.trimIndent(),
                LOG_CATEGORY
            )
        }
    }

    private fun createRouteRefreshCallback(
        oldRoute: NavigationRoute,
        currentLegIndex: Int,
        onRoutesRefreshed: (List<NavigationRoute>) -> Unit
    ) = object : NavigationRouterRefreshCallback {

        override fun onRefreshReady(route: NavigationRoute) {
            logI("Successful route refresh", LOG_CATEGORY)
            val routeDiffs = routeDiffProvider.buildRouteDiffs(
                oldRoute,
                route,
                currentLegIndex,
            )
            if (routeDiffs.isEmpty()) {
                logI("No changes to route annotations", LOG_CATEGORY)
            } else {
                for (diff in routeDiffs) {
                    logI(diff, LOG_CATEGORY)
                }
            }
            val directionsSessionRoutes = directionsSession.routes.toMutableList()
            if (directionsSessionRoutes.isNotEmpty()) {
                directionsSessionRoutes[0] = route
                onRoutesRefreshed(directionsSessionRoutes)
            }
            currentRequestId = null
        }

        override fun onFailure(error: NavigationRouterRefreshError) {
            logE(
                "Route refresh error: ${error.message} throwable=${error.throwable}",
                LOG_CATEGORY
            )
            currentRequestId = null
        }
    }
}
