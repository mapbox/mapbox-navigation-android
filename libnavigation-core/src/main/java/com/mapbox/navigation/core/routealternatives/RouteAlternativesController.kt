package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArraySet

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    private val navigator: MapboxNativeNavigator,
    private val tripSession: TripSession
) {

    private val nativeRouteAlternativesController = navigator.createRouteAlternativesController()
        .apply {
            setRouteAlternativesOptions(
                com.mapbox.navigator.RouteAlternativesOptions(
                    options.intervalMillis * SECONDS_PER_MILLIS,
                    MIN_TIME_BEFORE_MANEUVER_SECONDS,
                    LOOK_AHEAD_SECONDS
                )
            )
            enableEmptyAlternativesRefresh(true)
        }

    private val observers = CopyOnWriteArraySet<RouteAlternativesObserver>()

    fun register(routeAlternativesObserver: RouteAlternativesObserver) {
        val isStopped = observers.isEmpty()
        observers.add(routeAlternativesObserver)
        if (isStopped) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
        }
    }

    fun unregister(routeAlternativesObserver: RouteAlternativesObserver) {
        observers.remove(routeAlternativesObserver)
        if (observers.isEmpty()) {
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }
    }

    fun triggerAlternativeRequest() {
        nativeRouteAlternativesController.refreshImmediately()
    }

    fun unregisterAll() {
        nativeRouteAlternativesController.removeAllObservers()
        observers.clear()
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<com.mapbox.navigator.RouteAlternative>
        ): List<Int> {
            val routeProgress = tripSession.getRouteProgress()
                ?: return emptyList()

            // Map the alternatives from nav-native, add the existing RouteOptions.

            // TODO make async
            val alternatives: List<DirectionsRoute> = runBlocking {
                routeAlternatives.map { routeAlternative ->
                    parseDirectionsResponse(
                        routeAlternative.routeResponse,
                        routeProgress.route.routeOptions()
                    ) {
                        logI(TAG, Message("Response metadata: $it"))
                    }.first()
                }
            }

            // Notify the listeners.
            // TODO https://github.com/mapbox/mapbox-navigation-native/issues/4409
            // There is no way to determine if the route was Onboard or Offboard
            observers.forEach {
                it.onRouteAlternatives(routeProgress, alternatives, RouterOrigin.Onboard)
            }

            // This is supposed to be able to filter alternatives. If we want to provide
            // a mechanism to let downstream developers edit the routes - we should remove
            // the call to directionsSession.setRoutes
            return emptyList()
        }

        override fun onError(message: String) {
            logE(TAG, Message("Error: $message"))
        }
    }

    private companion object {
        private val TAG = Tag("MbxRouteAlternativesController")
        private const val MIN_TIME_BEFORE_MANEUVER_SECONDS = 1.0
        private const val LOOK_AHEAD_SECONDS = 1.0
        private const val SECONDS_PER_MILLIS = 0.001
    }
}
