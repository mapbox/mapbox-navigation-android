package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.LoggerProvider.logger
import com.mapbox.navigation.utils.internal.MapboxTimer
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

internal class RouteAlternativesController(
    private val options: RouteAlternativesOptions,
    private val navigator: MapboxNativeNavigator,
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val routeOptionsUpdater: RouteOptionsUpdater
) {
    private val jobControl = ThreadController.getMainScopeAndRootJob()

    private val mapboxTimer = MapboxTimer().apply {
        restartAfterMillis = options.intervalMillis
    }
    private val observers = CopyOnWriteArraySet<RouteAlternativesObserver>()
    private var currentRequestId: Long? = null

    fun register(routeAlternativesObserver: RouteAlternativesObserver) {
        val needsToStartTimer = observers.isEmpty()
        observers.add(routeAlternativesObserver)
        if (needsToStartTimer) {
            restartTimer()
        }
    }

    fun unregister(routeAlternativesObserver: RouteAlternativesObserver) {
        observers.remove(routeAlternativesObserver)
        if (observers.isEmpty()) {
            stop()
        }
    }

    fun unregisterAll() {
        observers.clear()
        stop()
    }

    private fun stop() {
        mapboxTimer.stopJobs()
        jobControl.job.cancelChildren()
        currentRequestId?.let { directionsSession.cancelRouteRequest(it) }
    }

    private fun requestRouteAlternatives() {
        if (directionsSession.routes.isEmpty() ||
            tripSession.getState() != TripSessionState.STARTED
        ) {
            return
        }

        val routeOptionsResult = routeOptionsUpdater.update(
            directionsSession.getPrimaryRouteOptions(),
            tripSession.getRouteProgress(),
            tripSession.getEnhancedLocation()
        )

        when (routeOptionsResult) {
            is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                currentRequestId?.let { directionsSession.cancelRouteRequest(it) }
                currentRequestId = directionsSession.requestRoutes(
                    routeOptionsResult.routeOptions,
                    routesRequestCallback
                )
            }
            is RouteOptionsUpdater.RouteOptionsResult.Error -> {
                logger.e(
                    msg = Message("Route alternatives options are not available"),
                    tr = routeOptionsResult.error
                )
            }
        }
    }

    fun interrupt() {
        currentRequestId?.let { directionsSession.cancelRouteRequest(it) }
        if (observers.isNotEmpty()) {
            restartTimer()
        }
    }

    private fun restartTimer() {
        mapboxTimer.stopJobs()
        mapboxTimer.startTimer {
            requestRouteAlternatives()
        }
    }

    private val routesRequestCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            val routeProgress = tripSession.getRouteProgress()
                ?: return
            jobControl.scope.launch {
                if (currentRequestId == null ||
                    tripSession.getState() == TripSessionState.STARTED
                ) {
                    val alternatives = routes.filter { navigator.isDifferentRoute(it) }
                    observers.forEach {
                        it.onRouteAlternatives(routeProgress, alternatives, routerOrigin)
                    }
                }
            }
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            logger.e(
                msg = Message("Route alternatives request failed")
            )
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            logger.w(msg = Message("Route alternatives request canceled"))
        }
    }
}
