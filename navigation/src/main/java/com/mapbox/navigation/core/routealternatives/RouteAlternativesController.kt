package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.parsing.models.nn.RouteInterfacesParser
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteIntersection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal typealias UpdateRoutesSuggestionObserver = (UpdateRouteSuggestion) -> Unit

internal class RouteAlternativesController(
    private val options: RouteAlternativesOptions,
    navigator: MapboxNativeNavigator,
    private val tripSession: TripSession,
    private val threadController: ThreadController,
    private val routeInterfacesParser: RouteInterfacesParser,
) : AlternativeMetadataProvider {

    private val mainJobControl by lazy { threadController.getMainScopeAndRootJob() }

    private var observerProcessingJob: Job? = null

    private var nativeRouteAlternativesController = setupNativeController(navigator)

    fun navigatorUpdated(navigator: MapboxNativeNavigator) {
        if (isCaRunning()) {
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }

        nativeRouteAlternativesController = setupNativeController(navigator)

        if (isCaRunning()) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
        }
    }

    private fun setupNativeController(
        navigator: MapboxNativeNavigator,
    ) = navigator.routeAlternativesController.apply {
        setRouteAlternativesOptions(
            com.mapbox.navigator.RouteAlternativesOptions(
                TimeUnit.MILLISECONDS.toSeconds(options.intervalMillis).toShort(),
                options.avoidManeuverSeconds.toFloat(),
            ),
        )
    }

    private val metadataMap = mutableMapOf<String, AlternativeRouteMetadata>()

    private var active = true
    private var defaultAlternativesHandler: RouteAlternativesToRouteUpdateSuggestionsAdapter? = null

    fun pause() {
        updateNativeObserver {
            active = false
        }
    }

    fun resume() {
        updateNativeObserver {
            active = true
        }
    }

    fun setRouteUpdateSuggestionListener(listener: UpdateRoutesSuggestionObserver?) {
        updateNativeObserver {
            defaultAlternativesHandler = if (listener == null) {
                null
            } else {
                RouteAlternativesToRouteUpdateSuggestionsAdapter(listener)
            }
        }
    }

    fun unregisterAll() {
        nativeRouteAlternativesController.removeAllObservers()
        defaultAlternativesHandler = null
        observerProcessingJob?.cancel()
    }

    override fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private fun updateNativeObserver(block: () -> Unit) {
        val wasRunning = isCaRunning()
        block()
        val shouldBeRunning = isCaRunning()
        if (shouldBeRunning && !wasRunning) {
            nativeRouteAlternativesController.addObserver(nativeObserver)
        }
        if (!shouldBeRunning && wasRunning) {
            nativeRouteAlternativesController.removeObserver(nativeObserver)
        }
    }

    private val nativeObserver = object : RouteAlternativesObserver {
        @Deprecated("Deprecated in Java")
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>,
        ) {
        }

        @Deprecated("Deprecated in Java")
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
            ) { alternatives ->
                logD("${alternatives.size} alternatives available", LOG_CATEGORY)

                val routeProgress = tripSession.getRouteProgress() ?: run {
                    logD("skipping alternatives update - no progress", LOG_CATEGORY)
                    return@processRouteAlternatives
                }

                val matchingAlternatives = alternatives
                    .filterMatchingUpcomingWaypoints(routeProgress)
                defaultAlternativesHandler?.onRouteAlternatives(
                    routeProgress,
                    matchingAlternatives,
                )
            }
        }

        override fun onError(message: String) {
            logE(LOG_CATEGORY) {
                "error in native RouteAlternativesObserver: $message"
            }
        }
    }

    /**
     * @param block invoked with results (on the main thread)
     */
    private fun processRouteAlternatives(
        onlinePrimaryRoute: RouteInterface?,
        nativeAlternatives: List<RouteAlternative>,
        block: suspend (List<NavigationRoute>) -> Unit,
    ) = mainJobControl.scope.launch {
        val primaryRoutes = onlinePrimaryRoute?.let { listOf(it) } ?: emptyList()
        val allAlternatives = primaryRoutes + nativeAlternatives.map { it.route }

        val alternatives = if (allAlternatives.isNotEmpty()) {
            val alternativesParsingResult = routeInterfacesParser.parserContinuousAlternatives(
                allAlternatives,
            )

            val expected = when (alternativesParsingResult) {
                AlternativesParsingResult.NotActual -> {
                    Result.failure(
                        Throwable("cancelled because another parsing is already in progress"),
                    )
                }

                is AlternativesParsingResult.Parsed -> alternativesParsingResult.value
            }

            expected.map { it.routes }.getOrElse {
                logE(
                    """
                        |unable to parse alternatives;
                        |failure for response with uuid: 
                        |${nativeAlternatives.firstOrNull()?.route?.responseUuid}
                    """.trimMargin(),
                    LOG_CATEGORY,
                )
                null
            }
        } else {
            emptyList()
        } ?: return@launch

        processAlternativesMetadata(alternatives, nativeAlternatives)
        block(alternatives)
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

    private fun List<NavigationRoute>.filterMatchingUpcomingWaypoints(
        routeProgress: RouteProgress,
    ): List<NavigationRoute> {
        val primaryUpcomingWaypoints = routeProgress.navigationRoute
            .internalWaypoints()
            .takeLast(routeProgress.remainingWaypoints)
            .filter { it.type == Waypoint.REGULAR || it.type == Waypoint.SILENT }
        return filter { alternative ->
            // Continuous alternatives are generated from the current position,
            // so every waypoint after the origin (index 0) is upcoming.
            val alternativeUpcomingWaypoints = alternative.internalWaypoints()
                .drop(1)
                .filter { it.type == Waypoint.REGULAR || it.type == Waypoint.SILENT }
            val matches = alternativeUpcomingWaypoints
                .matchesByLocationAndType(primaryUpcomingWaypoints)
            if (!matches) {
                logI(LOG_CATEGORY) {
                    "ignoring alternative ${alternative.id}: upcoming regular/silent " +
                        "waypoints don't match the current primary route"
                }
            }
            matches
        }
    }

    private class RouteAlternativesToRouteUpdateSuggestionsAdapter(
        private val suggestRouteUpdate: (UpdateRouteSuggestion) -> Unit,
    ) {
        fun onRouteAlternatives(
            routeProgress: RouteProgress,
            alternatives: List<NavigationRoute>,
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
    }

    private fun isCaRunning(): Boolean =
        active && defaultAlternativesHandler != null

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

private fun List<Waypoint>.matchesByLocationAndType(other: List<Waypoint>): Boolean {
    if (size != other.size) return false
    return withIndex().all { (index, waypoint) ->
        val otherWaypoint = other[index]
        waypoint.location == otherWaypoint.location && waypoint.type == otherWaypoint.type
    }
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
