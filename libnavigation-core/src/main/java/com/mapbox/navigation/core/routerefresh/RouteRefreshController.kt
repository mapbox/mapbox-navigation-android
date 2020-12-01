package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.internal.extensions.supportsRefresh
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.MapboxTimer
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 *
 * If the route is successfully refreshed, this class will update the [TripSession.route]
 *
 * [start] and [stop] are attached to the application lifecycle. Observing routes that
 * can be refreshed are handled by this class. Calling [start] will restart the refresh timer.
 */
internal class RouteRefreshController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val logger: Logger
) {

    private val routerRefreshTimer = MapboxTimer()

    init {
        routerRefreshTimer.restartAfterMillis = TimeUnit.MINUTES.toMillis(5)
    }

    fun start(): Job {
        stop()
        return routerRefreshTimer.startTimer {
            val route = tripSession.route?.takeIf { it.routeOptions().supportsRefresh() }
            route?.let {
                val legIndex = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0
                directionsSession.requestRouteRefresh(
                    route,
                    legIndex,
                    routeRefreshCallback
                )
            }
        }
    }

    fun stop() {
        routerRefreshTimer.stopJobs()
    }

    private val routeRefreshCallback = object : RouteRefreshCallback {

        override fun onRefresh(directionsRoute: DirectionsRoute) {
            logger.i(msg = Message("Successful route refresh"))
            tripSession.route = directionsRoute
            val directionsSessionRoutes = directionsSession.routes.toMutableList()
            if (directionsSessionRoutes.isNotEmpty()) {
                directionsSessionRoutes[0] = directionsRoute
                directionsSession.routes = directionsSessionRoutes
            }
        }

        override fun onError(error: RouteRefreshError) {
            logger.i(
                msg = Message("Route refresh error"),
                tr = error.throwable
            )
        }
    }
}
