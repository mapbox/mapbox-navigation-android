package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseNativeDirectionsAlternative
import com.mapbox.navigation.base.internal.utils.parseRouteInterface
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteIntersection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController
) : AlternativeMetadataProvider {

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
                    // Switch from offline to online primary route isn't implemented for the case
                    // when user manually triggers alternatives refresh
                    processRouteAlternatives(null, value) { alternatives, origin ->
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

    override fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ) { }

        override fun onOnlinePrimaryRouteAvailable(onlinePrimaryRoute: RouteInterface) {}

        override fun onRouteAlternativesUpdated(
            onlinePrimaryRoute: RouteInterface?,
            routeAlternatives: MutableList<RouteAlternative>,
            removedAlternatives: MutableList<RouteAlternative>
        ) {
            logI(LOG_CATEGORY) {
                "native alternatives available: ${routeAlternatives.map { it.route.routeId }}"
            }

            observerProcessingJob?.cancel()
            observerProcessingJob =
                processRouteAlternatives(
                    onlinePrimaryRoute,
                    routeAlternatives
                ) { alternatives, origin ->
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
        onlinePrimaryRoute: RouteInterface?,
        nativeAlternatives: List<RouteAlternative>,
        block: suspend (List<NavigationRoute>, RouterOrigin) -> Unit,
    ) = mainJobControl.scope.launch {
        val responseTimeElapsedSeconds = Time.SystemClockImpl.seconds()
        val alternatives: List<NavigationRoute> =
            nativeAlternatives.mapIndexedNotNull { index, routeAlternative ->
                val expected = withContext(ThreadController.DefaultDispatcher) {
                    parseNativeDirectionsAlternative(routeAlternative, responseTimeElapsedSeconds)
                }
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
        val newAlternatives = parseRouteInterfaceOrEmptyList(
            onlinePrimaryRoute,
            responseTimeElapsedSeconds
        ) + alternatives
        val origin = nativeAlternatives.find {
            // looking for the first new route,
            // assuming all new routes come from the same request
            it.isNew
        }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
        block(newAlternatives, origin)
        lastUpdateOrigin = origin
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

internal fun RouteAlternative.mapToMetadata(
    navigationRoute: NavigationRoute
): AlternativeRouteMetadata {
    return AlternativeRouteMetadata(
        navigationRoute = navigationRoute,
        forkIntersectionOfAlternativeRoute = alternativeRouteFork.mapToPlatform(),
        forkIntersectionOfPrimaryRoute = mainRouteFork.mapToPlatform(),
        infoFromFork = infoFromFork.mapToPlatform(),
        infoFromStartOfPrimary = infoFromStart.mapToPlatform(),
        alternativeId = id
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

private fun parseRouteInterfaceOrEmptyList(
    onlinePrimaryRoute: RouteInterface?,
    responseTimeElapsedSeconds: Long
) = onlinePrimaryRoute?.let {
    parseRouteInterface(it, responseTimeElapsedSeconds)
}?.value?.let { listOf(it) } ?: emptyList()
