package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.MapboxTimer
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.concurrent.TimeUnit

internal class FasterRouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val logger: Logger
) {

    private val fasterRouteTimer = MapboxTimer()
    private val fasterRouteDetector = FasterRouteDetector()
    private var fasterRouteObserver: FasterRouteObserver? = null

    fun attach(fasterRouteObserver: FasterRouteObserver) {
        val previousFasterRouteObserver = this.fasterRouteObserver
        this.fasterRouteObserver = fasterRouteObserver
        if (previousFasterRouteObserver == null) {
            val restartAfterMillis = fasterRouteObserver.restartAfterMillis()
            check(TimeUnit.MILLISECONDS.toMinutes(restartAfterMillis) >= 2) {
                "Faster route should be >= 2 minutes, $restartAfterMillis is out of range"
            }
            fasterRouteTimer.restartAfterMillis = restartAfterMillis
            fasterRouteTimer.startTimer {
                requestFasterRoute()
            }
        }
    }

    fun stop() {
        this.fasterRouteObserver = null
        fasterRouteTimer.stopJobs()
    }

    private fun requestFasterRoute() {
        val restartAfterMillis = fasterRouteObserver?.restartAfterMillis()
            ?: return
        if (directionsSession.routes.isEmpty()) {
            return
        }

        fasterRouteTimer.restartAfterMillis = restartAfterMillis
        ifNonNull(tripSession.getEnhancedLocation()) { enhancedLocation ->
            val optionsRebuilt = AdjustedRouteOptionsProvider.getRouteOptions(directionsSession, tripSession, enhancedLocation)
                ?: return
            directionsSession.requestFasterRoute(optionsRebuilt, fasterRouteRequestCallback)
        }
    }

    private val fasterRouteRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            val currentRoute = directionsSession.routes.firstOrNull()
                ?: return
            tripSession.getRouteProgress()?.let { progress ->
                val isAlternativeFaster = fasterRouteDetector.isRouteFaster(routes[0], progress)
                fasterRouteObserver?.onFasterRoute(currentRoute, routes, isAlternativeFaster)
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            logger.i(
                msg = Message("Faster route request failed"),
                tr = throwable
            )
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            logger.i(msg = Message("Faster route request canceled"))
        }
    }
}
