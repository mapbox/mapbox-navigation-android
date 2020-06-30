package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsProvider
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.MapboxTimer
import com.mapbox.navigation.utils.internal.ThreadController
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

internal class FasterRouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val routeOptionsProvider: RouteOptionsProvider,
    private val fasterRouteDetector: FasterRouteDetector,
    private val logger: Logger
) {

    private val jobControl = ThreadController.getMainScopeAndRootJob()

    private val fasterRouteTimer = MapboxTimer()
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
        jobControl.job.cancelChildren()
    }

    private fun requestFasterRoute() {
        val restartAfterMillis = fasterRouteObserver?.restartAfterMillis()
            ?: return
        if (directionsSession.routes.isEmpty()) {
            return
        }

        fasterRouteTimer.restartAfterMillis = restartAfterMillis

        routeOptionsProvider.update(
            directionsSession.getRouteOptions(),
            tripSession.getRouteProgress(),
            tripSession.getEnhancedLocation()
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsProvider.RouteOptionsResult.Success ->
                        directionsSession.requestFasterRoute(
                            routeOptionsResult.routeOptions,
                            fasterRouteRequestCallback
                        )
                    is RouteOptionsProvider.RouteOptionsResult.Error -> Unit
                }
            }
    }

    private val fasterRouteRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            val currentRoute = directionsSession.routes.firstOrNull()
                ?: return
            val routeProgress = tripSession.getRouteProgress()
                ?: return
            jobControl.scope.launch {
                val isAlternativeFaster = fasterRouteDetector.isRouteFaster(routes[0], routeProgress)
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
