package com.mapbox.navigation.core.routealternatives

import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RoutingMode
import com.mapbox.navigator.RoutingProfile
import okhttp3.internal.toImmutableList
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    private val navigator: MapboxNativeNavigator,
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession
) {
    private val nativeRouteAlternativesController = navigator.createRouteAlternativesController()
        .apply {
            setRouteProfile(
                RoutingProfile(
                    RoutingMode.DRIVING_TRAFFIC,
                    "What is this account"
                )
            )
            setRouteAlternativesOptions(
                com.mapbox.navigator.RouteAlternativesOptions(
                    options.intervalMillis * 0.001,
                    minTimeBeforeManeuverSeconds,
                    lookAheadSeconds)
            )
            enableEmptyAlternativesRefresh(true)
        }

    private var alternatives: List<RouteAlternative> = emptyList()
    private val observers = CopyOnWriteArraySet<RouteAlternativesObserver>()

    fun register(routeAlternativesObserver: RouteAlternativesObserver) {
        val isStopped = observers.isEmpty()
        observers.add(routeAlternativesObserver)
        if (isStopped) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
            nativeRouteAlternativesController.start()
        }
    }

    fun unregister(routeAlternativesObserver: RouteAlternativesObserver) {
        observers.remove(routeAlternativesObserver)
        if (observers.isEmpty()) {
            nativeRouteAlternativesController.stop()
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }
    }

    fun triggerAlternativeRequest() {
        nativeRouteAlternativesController.refreshImmediately()
    }

    fun unregisterAll() {
        nativeRouteAlternativesController.removeAllObservers()
        observers.clear()
        nativeRouteAlternativesController.stop()
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver() {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<com.mapbox.navigator.RouteAlternative>
        ): List<Int> {
            Log.i("kyle_debug", "nativeObserver onRouteAlternativesChanged ${routeAlternatives.size}")
            val routeProgress = tripSession.getRouteProgress()
            val alternatives = RouteAlternativeMapper.from(routeAlternatives)
            this@RouteAlternativesController.alternatives = alternatives
            val navigationRoute = directionsSession.navigationRoute
            val activeRoutes = mutableListOf<DirectionsRoute>()
            routeProgress?.route?.let { activeRoutes.add(it) }
            alternatives.forEach { routeAlternative ->
                activeRoutes.add(routeAlternative.directionsRoute)
            }
            directionsSession.setRoutes(navigationRoute?.copy(activeRoutes = activeRoutes))
            observers.forEach {
                it.onRouteAlternatives(routeProgress, alternatives)
            }
            return emptyList()
        }
    }

    private companion object {
        private const val minTimeBeforeManeuverSeconds = 1.0
        private const val lookAheadSeconds = 1.0
    }
}
