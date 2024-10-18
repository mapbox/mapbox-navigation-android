package com.mapbox.navigation.core.routealternatives

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
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.findRoute
import com.mapbox.navigation.core.internal.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteIntersection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal typealias UpdateRoutesSuggestionObserver = (UpdateRouteSuggestion) -> Unit

internal class RouteAlternativesController(
    private val options: RouteAlternativesOptions,
    private val navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController,
    private val routeParsingManager: RouteParsingManager,
    private val directionSession: DirectionsSession,
) : AlternativeMetadataProvider {

    @RouterOrigin
    private var lastUpdateOrigin: String = RouterOrigin.OFFLINE

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    private var observerProcessingJob: Job? = null

    private val nativeRouteAlternativesController = navigator.routeAlternativesController
        .apply {
            setRouteAlternativesOptions(
                com.mapbox.navigator.RouteAlternativesOptions(
                    TimeUnit.MILLISECONDS.toSeconds(options.intervalMillis).toShort(),
                    options.avoidManeuverSeconds.toFloat(),
                ),
            )
        }

    private val metadataMap = mutableMapOf<String, AlternativeRouteMetadata>()
    private var defaultAlternativesHandler: RouteAlternativesToRouteUpdateSuggestionsAdapter? =
        null
    private var customAlternativesHandler: NavigationRouteAlternativesObserver? = null

    fun setRouteUpdateSuggestionListener(listener: UpdateRoutesSuggestionObserver?) {
        updateNativeObserver {
            defaultAlternativesHandler = if (listener == null) {
                null
            } else {
                RouteAlternativesToRouteUpdateSuggestionsAdapter(listener)
            }
        }
    }

    fun setRouteAlternativesObserver(
        routeAlternativesObserver: NavigationRouteAlternativesObserver,
    ) {
        updateNativeObserver {
            customAlternativesHandler = routeAlternativesObserver
        }
    }

    fun restoreDefaultRouteAlternativesObserver() {
        updateNativeObserver {
            customAlternativesHandler = null
        }
    }

    fun unregisterAll() {
        nativeRouteAlternativesController.removeAllObservers()
        customAlternativesHandler = null
        defaultAlternativesHandler = null
        observerProcessingJob?.cancel()
    }

    fun onEVDataUpdated(data: Map<String, String>) {
        navigator.routeAlternativesController.onEvDataUpdated(HashMap(data))
    }

    override fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private fun updateNativeObserver(block: () -> Unit) {
        val wasRunning = customAlternativesHandler != null || defaultAlternativesHandler != null
        block()
        val shouldBeRunning = customAlternativesHandler != null ||
            defaultAlternativesHandler != null
        if (shouldBeRunning && !wasRunning) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
        }
        if (!shouldBeRunning && wasRunning) {
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }
    }

    private val nativeObserver = object : RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>,
        ) {
        }

        override fun onOnlinePrimaryRouteAvailable(onlinePrimaryRoute: RouteInterface) {}

        override fun onRouteAlternativesUpdated(
            onlinePrimaryRoute: RouteInterface?,
            routeAlternatives: MutableList<RouteAlternative>,
            removedAlternatives: MutableList<RouteAlternative>,
        ) {
            logI(LOG_CATEGORY) {
                "native alternatives available: ${routeAlternatives.map { it.route.routeId }}"
            }

            observerProcessingJob?.cancel()
            observerProcessingJob = processRouteAlternatives(
                onlinePrimaryRoute,
                routeAlternatives,
            ) { alternatives, origin ->
                logD("${alternatives.size} alternatives available", LOG_CATEGORY)

                val routeProgress = tripSession.getRouteProgress() ?: run {
                    logD("skipping alternatives update - no progress", LOG_CATEGORY)
                    return@processRouteAlternatives
                }

                observerToTrigger?.onRouteAlternatives(routeProgress, alternatives, origin)
            }
        }

        override fun onError(message: String) {
            observerToTrigger?.onRouteAlternativesError(
                RouteAlternativesError(message = message),
            )
        }
    }

    private val observerToTrigger: NavigationRouteAlternativesObserver? get() =
        customAlternativesHandler ?: defaultAlternativesHandler

    /**
     * @param block invoked with results (on the main thread)
     */
    private fun processRouteAlternatives(
        onlinePrimaryRoute: RouteInterface?,
        nativeAlternatives: List<RouteAlternative>,
        block: suspend (List<NavigationRoute>, String) -> Unit,
    ) = mainJobControl.scope.launch {
        val responseTimeElapsedSeconds = Time.SystemClockImpl.seconds()

        val primaryRoutes = onlinePrimaryRoute?.let { listOf(it) } ?: emptyList()
        val allAlternatives = primaryRoutes + nativeAlternatives.map { it.route }

        val alternatives = if (allAlternatives.isNotEmpty()) {
            val args = AlternativesInfo(
                RouteResponseInfo.fromResponses(allAlternatives.map { it.responseJsonRef.buffer }),
            )

            val alternativesParsingResult = routeParsingManager.parseAlternatives(args) {
                withContext(ThreadController.DefaultDispatcher) {
                    parseRouteInterfaces(
                        routes = allAlternatives,
                        responseTimeElapsedSeconds = responseTimeElapsedSeconds,
                        routeLookup = directionSession::findRoute,
                        routeToDirections = routeParsingManager::parseRouteToDirections,
                    )
                }
            }

            val expected = when (alternativesParsingResult) {
                AlternativesParsingResult.NotActual -> {
                    ExpectedFactory.createError(
                        Throwable("cancelled because another parsing is already in progress"),
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
                    LOG_CATEGORY,
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

    @RouterOrigin
    private fun getOrigin(nativeAlternatives: List<RouteAlternative>): String {
        val origin = nativeAlternatives.find {
            // looking for the first new route,
            // assuming all new routes come from the same request
            it.isNew
        }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
        return origin
    }

    fun processAlternativesMetadata(
        routes: List<NavigationRoute>,
        nativeAlternatives: List<RouteAlternative>,
    ) {
        metadataMap.clear()
        nativeAlternatives.forEach { nativeAlternative ->
            routes.find { nativeAlternative.route.routeId == it.id }?.let { navigationRoute ->
                metadataMap[nativeAlternative.route.routeId] = nativeAlternative.mapToMetadata(
                    navigationRoute,
                )
            }
        }
    }

    private class RouteAlternativesToRouteUpdateSuggestionsAdapter(
        private val suggestRouteUpdate: (UpdateRouteSuggestion) -> Unit,
    ) : NavigationRouteAlternativesObserver {
        override fun onRouteAlternatives(
            routeProgress: RouteProgress,
            alternatives: List<NavigationRoute>,
            @RouterOrigin routerOrigin: String,
        ) {
            when (routeProgress.navigationRoute.origin) {
                RouterOrigin.ONLINE -> {
                    val onlineAlternatives = alternatives.filter {
                        it.origin == RouterOrigin.ONLINE
                    }
                    suggestRouteUpdate(
                        UpdateRouteSuggestion(
                            listOf(routeProgress.navigationRoute) + onlineAlternatives,
                            SuggestionType.AlternativesUpdated,
                        ),
                    )
                }

                RouterOrigin.OFFLINE -> {
                    val onlineAlternatives = alternatives.filter {
                        it.origin == RouterOrigin.ONLINE
                    }
                    val offlineAlternatives = alternatives.filter {
                        it.origin == RouterOrigin.OFFLINE
                    }
                    if (onlineAlternatives.isNotEmpty()) {
                        suggestRouteUpdate(
                            UpdateRouteSuggestion(
                                onlineAlternatives,
                                SuggestionType.SwitchToOnlineAlternative,
                            ),
                        )
                    } else if (offlineAlternatives.isNotEmpty()) {
                        suggestRouteUpdate(
                            UpdateRouteSuggestion(
                                listOf(routeProgress.navigationRoute) +
                                    offlineAlternatives,
                                SuggestionType.AlternativesUpdated,
                            ),
                        )
                    } else if (alternatives.isEmpty()) {
                        suggestRouteUpdate(
                            UpdateRouteSuggestion(
                                listOf(routeProgress.navigationRoute),
                                SuggestionType.AlternativesUpdated,
                            ),
                        )
                    }
                }
            }
        }

        override fun onRouteAlternativesError(error: RouteAlternativesError) {
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "RouteAlternativesController"
    }
}

internal data class UpdateRouteSuggestion(
    val newRoutes: List<NavigationRoute>,
    val type: SuggestionType,
)

internal sealed class SuggestionType {
    object AlternativesUpdated : SuggestionType()
    object SwitchToOnlineAlternative : SuggestionType()
}

internal fun RouteAlternative.mapToMetadata(
    navigationRoute: NavigationRoute,
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
