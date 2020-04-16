package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigation.utils.timer.MapboxTimer

internal class FasterRouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession
) {
    private val fasterRouteTimer = MapboxTimer()
    private val fasterRouteDetector = FasterRouteDetector()
    private var fasterRouteObserver: FasterRouteObserver? = null

    fun attach(fasterRouteObserver: FasterRouteObserver) {
        val previousFasterRouteObserver = this.fasterRouteObserver
        this.fasterRouteObserver = fasterRouteObserver
        if (previousFasterRouteObserver == null) {
            fasterRouteTimer.restartAfterMillis = fasterRouteObserver.restartAfterMillis()
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
                fasterRouteObserver?.onFasterRoute(currentRoute, routes[0], isAlternativeFaster)
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            // do nothing
            // todo log in the future
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            // do nothing
            // todo log in the future
        }
    }
}
