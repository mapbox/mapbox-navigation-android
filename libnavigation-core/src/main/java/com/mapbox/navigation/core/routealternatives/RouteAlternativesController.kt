package com.mapbox.navigation.core.routealternatives

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteAlternative
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArraySet

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    private val navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController
) {

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

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
            mainJobControl.job.cancelChildren()
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
            routeAlternatives: List<RouteAlternative>
        ): List<Int> {
            val routeProgress = tripSession.getRouteProgress()
                ?: return emptyList()

            onRouteAlternativesChanged(routeProgress, routeAlternatives)

            // This is supposed to be able to filter alternatives. If we want to provide
            // a mechanism to let downstream developers edit the routes - we should remove
            // the call to directionsSession.setRoutes
            return emptyList()
        }
    }

    private fun onRouteAlternativesChanged(
        routeProgress: RouteProgress,
        routeAlternatives: List<RouteAlternative>
    ) {
        // Map the alternatives from nav-native, add the existing RouteOptions.
        val alternatives = runBlocking {
            routeAlternatives
                .map { routeAlternative ->
                    parseDirectionsResponse(
                        routeAlternative.route,
                        routeProgress.route.routeOptions()
                    ) {
                        logI(TAG, Message("Response metadata: $it"))
                    }.first()
                }
        }

        mainJobControl.scope.launch {
            // Notify the listeners.
            // TODO https://github.com/mapbox/mapbox-navigation-native/issues/4409
            // There is no way to determine if the route was Onboard or Offboard
            observers.forEach {
                it.onRouteAlternatives(routeProgress, alternatives, RouterOrigin.Onboard)
            }
        }
    }

    private companion object {
        private val TAG = Tag("MbxRouteAlternativesController")
        private const val MIN_TIME_BEFORE_MANEUVER_SECONDS = 1.0
        private const val LOOK_AHEAD_SECONDS = 1.0
        private const val SECONDS_PER_MILLIS = 0.001
    }
}
