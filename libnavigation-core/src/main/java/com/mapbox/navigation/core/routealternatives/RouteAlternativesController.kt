package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.utils.parseNativeDirectionsAlternative
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.mapToSdkRouteOrigin
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteIntersection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController
) {

    private var lastUpdateOrigin: RouterOrigin = RouterOrigin.Onboard

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    private var observerProcessingJob: Job? = null

    private val nativeRouteAlternativesController = navigator.routeAlternativesController
        .apply {
            setRouteAlternativesOptions(
                com.mapbox.navigator.RouteAlternativesOptions(
                    TimeUnit.MILLISECONDS.toSeconds(options.intervalMillis).toDouble(),
                    options.avoidManeuverSeconds.toDouble()
                )
            )
            enableOnEmptyAlternativesRequest(true)
        }

    private val observers = CopyOnWriteArraySet<NavigationRouteAlternativesObserver>()

    private val legacyObserversMap =
        hashMapOf<RouteAlternativesObserver, NavigationRouteAlternativesObserver>()

    private val metadataMap = mutableMapOf<String, AlternativeRouteMetadata>()

    /**
     * This flag is used to conditionally ignore the calls to native `RouteAlternativesObserver`.
     *
     * It's needed because the native observer will fire every time new alternative routes are provided via [MapboxNavigation.setNavigationRoutes],
     * which would return back to developer the same alternatives they already set, unnecessary duplicating the work.
     *
     * This additional call to the native observer will be made synchronously when [Navigator.setAlternativeRoutes] is called.
     * To prevent this from happening, we're "pausing" the observer for the duration of the route updates.
     */
    private var paused = false

    fun register(routeAlternativesObserver: RouteAlternativesObserver) {
        val observer = object : NavigationRouteAlternativesObserver {
            override fun onRouteAlternatives(
                routeProgress: RouteProgress,
                alternatives: List<NavigationRoute>,
                routerOrigin: RouterOrigin
            ) {
                routeAlternativesObserver.onRouteAlternatives(
                    routeProgress,
                    alternatives.toDirectionsRoutes(),
                    routerOrigin
                )
            }

            override fun onRouteAlternativesError(error: RouteAlternativesError) {
                logE("Error: ${error.message}", LOG_CATEGORY)
            }
        }
        legacyObserversMap[routeAlternativesObserver] = observer
        register(observer)
    }

    fun unregister(routeAlternativesObserver: RouteAlternativesObserver) {
        val observer = legacyObserversMap.remove(routeAlternativesObserver)
        if (observer != null) {
            unregister(observer)
        }
    }

    fun register(routeAlternativesObserver: NavigationRouteAlternativesObserver) {
        val isStopped = observers.isEmpty()
        observers.add(routeAlternativesObserver)
        if (isStopped) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
        }
    }

    fun unregister(routeAlternativesObserver: NavigationRouteAlternativesObserver) {
        observers.remove(routeAlternativesObserver)
        if (observers.isEmpty()) {
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }
    }

    fun triggerAlternativeRequest(listener: NavigationRouteAlternativesRequestCallback?) {
        nativeRouteAlternativesController.refreshImmediately { expected ->
            val routeProgress = tripSession.getRouteProgress()
                ?: run {
                    listener?.onRouteAlternativesRequestError(
                        RouteAlternativesError(
                            message =
                            """
                                |Route progress not available, ignoring alternatives update.
                                |Continuous alternatives are only available in active guidance.
                            """.trimMargin()
                        )
                    )
                    return@refreshImmediately
                }

            expected.fold(
                { error ->
                    listener?.onRouteAlternativesRequestError(
                        // NN should expose origin of a failed alternatives request,
                        // refs https://github.com/mapbox/mapbox-navigation-native/issues/5401
                        RouteAlternativesError(
                            message = error
                        )
                    )
                },
                { value ->
                    processRouteAlternatives(value) { alternatives, origin ->
                        listener?.onRouteAlternativeRequestFinished(
                            routeProgress,
                            alternatives,
                            origin
                        )
                    }
                }
            )
        }
    }

    fun unregisterAll() {
        nativeRouteAlternativesController.removeAllObservers()
        observers.clear()
        legacyObserversMap.clear()
        observerProcessingJob?.cancel()
    }

    fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ): List<Int> {
            logD("${routeAlternatives.size} native alternatives available", LOG_CATEGORY)
            if (paused) {
                logD("paused, returning", LOG_CATEGORY)
                return emptyList()
            }

            observerProcessingJob?.cancel()
            observerProcessingJob =
                processRouteAlternatives(routeAlternatives) { alternatives, origin ->
                    logD("${alternatives.size} alternatives available", LOG_CATEGORY)

                    val routeProgress = tripSession.getRouteProgress()
                        ?: run {
                            logD("skipping alternatives update - no progress", LOG_CATEGORY)
                            return@processRouteAlternatives
                        }

                    observers.forEach {
                        it.onRouteAlternatives(routeProgress, alternatives, origin)
                    }
                }

            // This is supposed to be able to filter alternatives
            // but at this point we're not filtering anything.
            return emptyList()
        }

        override fun onError(message: String) {
            observers.forEach {
                // NN should expose origin of a failed alternatives request and the used URL,
                // refs https://github.com/mapbox/mapbox-navigation-native/issues/5401
                // and https://github.com/mapbox/mapbox-navigation-native/issues/5402
                it.onRouteAlternativesError(
                    RouteAlternativesError(message = message)
                )
            }
        }
    }

    /**
     * @param block invoked with results (on the main thread)
     */
    private fun processRouteAlternatives(
        nativeAlternatives: List<RouteAlternative>,
        block: (List<NavigationRoute>, RouterOrigin) -> Unit,
    ) = mainJobControl.scope.launch {
        val alternatives: List<NavigationRoute> =
            nativeAlternatives.mapIndexedNotNull { index, routeAlternative ->
                val expected = parseNativeDirectionsAlternative(
                    ThreadController.IODispatcher,
                    routeAlternative
                )
                if (expected.isValue) {
                    expected.value
                } else {
                    logE(
                        """
                                |unable to parse alternative at index $index;
                                |failure for response: ${routeAlternative.route.responseJson}
                        """.trimMargin(),
                        LOG_CATEGORY
                    )
                    null
                }
            }
        processAlternativesMetadata(alternatives, nativeAlternatives)

        val origin = nativeAlternatives.find {
            // looking for the first new route,
            // assuming all new routes come from the same request
            it.isNew
        }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
        block(alternatives, origin)
        lastUpdateOrigin = origin
    }

    /**
     * @see paused
     */
    fun pauseUpdates() {
        paused = true
    }

    /**
     * @see paused
     */
    fun resumeUpdates() {
        paused = false
    }

    fun processAlternativesMetadata(
        routes: List<NavigationRoute>,
        nativeAlternatives: List<RouteAlternative>
    ) {
        metadataMap.clear()
        nativeAlternatives.forEach { nativeAlternative ->
            routes.find { nativeAlternative.route.routeId == it.id }?.let { navigationRoute ->
                metadataMap[nativeAlternative.route.routeId] = nativeAlternative.mapToMetadata(
                    navigationRoute
                )
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "RouteAlternativesController"
    }
}

private fun RouteAlternative.mapToMetadata(
    navigationRoute: NavigationRoute
): AlternativeRouteMetadata {
    return AlternativeRouteMetadata(
        navigationRoute = navigationRoute,
        forkIntersectionOfAlternativeRoute = alternativeRouteFork.mapToPlatform(),
        forkIntersectionOfPrimaryRoute = mainRouteFork.mapToPlatform(),
        infoFromFork = infoFromFork.mapToPlatform(),
        infoFromStartOfPrimary = infoFromStart.mapToPlatform(),
    )
}

private fun RouteIntersection.mapToPlatform(): AlternativeRouteIntersection {
    return AlternativeRouteIntersection(
        location = location,
        geometryIndexInRoute = geometryIndex,
        geometryIndexInLeg = segmentIndex,
        legIndex = legIndex,
    )
}

private fun com.mapbox.navigator.AlternativeRouteInfo.mapToPlatform(): AlternativeRouteInfo {
    return AlternativeRouteInfo(
        distance = distance,
        duration = duration,
    )
}
