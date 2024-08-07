package com.mapbox.navigation.core.routealternatives

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.utils.AlternativesInfo
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.RouteParsingManager
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseRouteInterfaces
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
    private val navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController,
    private val routeParsingManager: RouteParsingManager
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
                    processRouteAlternatives(
                        null,
                        value,
                        immediateAlternativesRefresh = true,
                        notActualParsingCallback = {
                            listener?.onRouteAlternativeRequestFinished(
                                routeProgress,
                                emptyList(),
                                it
                            )
                        }
                    ) { alternatives, origin ->
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

    fun onEVDataUpdated(data: Map<String, String>) {
        navigator.routeAlternativesController.onEvDataUpdated(HashMap(data))
    }

    override fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ) {
        }

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
                    routeAlternatives,
                    notActualParsingCallback = { },
                    immediateAlternativesRefresh = false
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
        immediateAlternativesRefresh: Boolean,
        notActualParsingCallback: (RouterOrigin) -> Unit,
        block: suspend (List<NavigationRoute>, RouterOrigin) -> Unit,
    ) = mainJobControl.scope.launch {
        val responseTimeElapsedSeconds = Time.SystemClockImpl.seconds()

        val primaryRoutes = onlinePrimaryRoute?.let { listOf(it) } ?: emptyList()
        val allAlternatives = primaryRoutes + nativeAlternatives.map { it.route }

        val alternatives: List<NavigationRoute> = if (allAlternatives.isNotEmpty()) {
            val args = AlternativesInfo(
                RouteResponseInfo.fromResponses(allAlternatives.map { it.responseJsonRef.buffer }),
                userTriggeredAlternativesRefresh = immediateAlternativesRefresh
            )
            val alternativesParsingResult:
                AlternativesParsingResult<Expected<Throwable, List<NavigationRoute>>> =
                routeParsingManager.parseAlternatives(args) { parseArgs ->
                    withContext(ThreadController.DefaultDispatcher) {
                        parseRouteInterfaces(
                            allAlternatives,
                            responseTimeElapsedSeconds,
                            parseArgs
                        )
                    }
                }
            val expected = when (alternativesParsingResult) {
                AlternativesParsingResult.NotActual -> {
                    notActualParsingCallback(getOrigin(nativeAlternatives))
                    ExpectedFactory.createError(
                        Throwable("cancelled because another parsing is already in progress")
                    )
                }
                is AlternativesParsingResult.Parsed -> alternativesParsingResult.value
            }

            if (expected.isValue) {
                expected.value
            } else {
                logE(
                    """
                        |unable to parse alternatives;
                        |failure for response with uuid: 
                        |${nativeAlternatives.firstOrNull()?.route?.responseUuid}
                    """.trimMargin(),
                    LOG_CATEGORY
                )
                null
            } ?: return@launch
        } else {
            emptyList()
        }

        processAlternativesMetadata(alternatives, nativeAlternatives)
        val origin = getOrigin(nativeAlternatives)
        block(alternatives, origin)
        lastUpdateOrigin = origin
    }

    private fun getOrigin(nativeAlternatives: List<RouteAlternative>): RouterOrigin {
        val origin = nativeAlternatives.find {
            // looking for the first new route,
            // assuming all new routes come from the same request
            it.isNew
        }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
        return origin
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
