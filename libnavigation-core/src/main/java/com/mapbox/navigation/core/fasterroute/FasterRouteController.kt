package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.timer.MapboxTimer
import java.util.concurrent.CopyOnWriteArrayList

internal class FasterRouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession
) {
    private val fasterRouteTimer = MapboxTimer()
    private val fasterRouteObservers = CopyOnWriteArrayList<FasterRouteObserver>()

    fun attach(fasterRouteObserver: FasterRouteObserver) {
        if (fasterRouteObservers.isEmpty()) {
            fasterRouteTimer.startTimer {
                requestFasterRoute()
            }
        }
        fasterRouteObservers.add(fasterRouteObserver)
    }

    fun detach(fasterRouteObserver: FasterRouteObserver) {
        fasterRouteObservers.remove(fasterRouteObserver)
        if (fasterRouteObservers.isEmpty()) {
            fasterRouteTimer.stopJobs()
        }
    }

    fun stop() {
        if (!fasterRouteObservers.isEmpty()) {
            fasterRouteObservers.clear()
        }
        fasterRouteTimer.stopJobs()
    }

    private fun requestFasterRoute() {
        ifNonNull(
            directionsSession.getRouteOptions(),
            tripSession.getEnhancedLocation()
        ) { options, enhancedLocation ->
            val routeProgress = tripSession.getRouteProgress() ?: return
            val optionsRebuilt = directionsSession.getAdjustedRouteOptions(options, routeProgress, enhancedLocation)
            directionsSession.requestFasterRoute(optionsRebuilt, fasterRouteRequestCallback)
        }
    }

    private val fasterRouteRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            tripSession.getRouteProgress()?.let { progress ->
                if (FasterRouteDetector.isRouteFaster(routes[0], progress)) {
                    fasterRouteObservers.forEach { it.onFasterRouteAvailable(routes[0]) }
                }
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
