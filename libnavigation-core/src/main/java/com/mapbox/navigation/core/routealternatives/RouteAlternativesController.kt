package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.NavigationRouteProvider
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseNativeDirectionsAlternative
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteIntersection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal class RouteAlternativesController constructor(
    private val options: RouteAlternativesOptions,
    navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController,
    val allAlternativesObserversHolder: AllAlternativesObserversHolder,
) : FirstAndLastObserverListener {

    init {
        allAlternativesObserversHolder.addFirstAndLastObserverListener(this)
    }

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

    private val metadataMap = mutableMapOf<String, AlternativeRouteMetadata>()

    override fun onFirstObserver() {
        nativeRouteAlternativesController.addObserver(nativeObserver)
    }

    override fun onLastObserver() {
        nativeRouteAlternativesController.removeObserver(nativeObserver)
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

    fun clear() {
        nativeRouteAlternativesController.removeAllObservers()
        allAlternativesObserversHolder.clear()
        observerProcessingJob?.cancel()
    }

    fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ) {
            logI("${routeAlternatives.size} native alternatives available", LOG_CATEGORY)

            observerProcessingJob?.cancel()
            observerProcessingJob =
                processRouteAlternatives(routeAlternatives) { alternatives, origin ->
                    logD("${alternatives.size} alternatives available", LOG_CATEGORY)

                    val routeProgress = tripSession.getRouteProgress()
                        ?: run {
                            logD("skipping alternatives update - no progress", LOG_CATEGORY)
                            return@processRouteAlternatives
                        }
                    allAlternativesObserversHolder.onRouteAlternatives(
                        routeProgress,
                        alternatives,
                        origin
                    )
                }
        }

        override fun onOnlinePrimaryRouteAvailable(onlinePrimaryRoute: RouteInterface) {
            logI("onOnlinePrimaryRouteAvailable: route ${onlinePrimaryRoute.routeId}", LOG_CATEGORY)
            val navigationRoute = NavigationRouteProvider.createSingleRoute(onlinePrimaryRoute)
            if (navigationRoute != null) {
                allAlternativesObserversHolder.onOffboardRoutesAvailable(listOf(navigationRoute))
            } else {
                logW(
                    "Could not parse native online route ${onlinePrimaryRoute.routeId}",
                    LOG_CATEGORY
                )
            }
        }

        override fun onError(message: String) {
            // NN should expose origin of a failed alternatives request and the used URL,
            // refs https://github.com/mapbox/mapbox-navigation-native/issues/5401
            // and https://github.com/mapbox/mapbox-navigation-native/issues/5402
            allAlternativesObserversHolder.onRouteAlternativesError(
                RouteAlternativesError(message = message)
            )
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
                    ThreadController.DefaultDispatcher,
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

    internal companion object {
        internal const val LOG_CATEGORY = "RouteAlternativesController"
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
