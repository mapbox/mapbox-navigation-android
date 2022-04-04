package com.mapbox.navigation.core.routealternatives

import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.utils.parseNativeDirectionsAlternative
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.mapToSdkRouteOrigin
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteIntersection
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

    private var metadataMap = mapOf<String, AlternativeRouteMetadata>()

    private val metadataObservers = CopyOnWriteArraySet<AlternativeRouteMetadataObserver>()

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

    fun registerMetadataObserver(metadataObserver: AlternativeRouteMetadataObserver) {
        metadataObservers.add(metadataObserver)
        metadataObserver.onMetadataUpdated(metadataMap.values.toList())
    }

    fun unregisterMetadataObserver(metadataObserver: AlternativeRouteMetadataObserver) {
        metadataObservers.remove(metadataObserver)
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
        metadataObservers.clear()
    }

    fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return metadataMap[navigationRoute.id]
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ): List<Int> {
            Log.e("lp_test", "nativeObserver#onRouteAlternativesChanged")
            if (paused) {
                Log.e("lp_test", "nativeObserver#onRouteAlternativesChanged - paused")
                return emptyList()
            }

            processRouteAlternatives(routeAlternatives) { alternatives, origin ->
                logD("${alternatives.size} alternatives available", LOG_CATEGORY)

                val routeProgress = tripSession.getRouteProgress()
                    ?: return@processRouteAlternatives

                logD("${alternatives.size} alternatives delivered", LOG_CATEGORY)
                observers.forEach {
                    it.onRouteAlternatives(routeProgress, alternatives, origin)
                }
            }

            // This is supposed to be able to filter alternatives
            // but at this point we're not filtering anything.
            return emptyList()
            /*return mutableListOf<Int>().apply {
                var index = 0
                repeat(routeAlternatives.size) { add(index++) }
            }*/
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
    ) {
        mainJobControl.scope.launch {
            val localMetadataMap = mutableMapOf<String, AlternativeRouteMetadata>()
            val alternatives: List<NavigationRoute> =
                nativeAlternatives.mapIndexedNotNull { index, routeAlternative ->
                    val expected = parseNativeDirectionsAlternative(
                        ThreadController.IODispatcher,
                        routeAlternative
                    )
                    if (expected.isValue) {
                        expected.value?.also {
                            localMetadataMap[routeAlternative.route.routeId] =
                                routeAlternative.mapToMetadata(it)
                        }
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
            notifyMetadataObservers(localMetadataMap)

            val origin = nativeAlternatives.find {
                // looking for the first new route,
                // assuming all new routes come from the same request
                it.isNew
            }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
            block(alternatives, origin)
            lastUpdateOrigin = origin
        }
    }

    private fun notifyMetadataObservers(update: Map<String, AlternativeRouteMetadata>) {
        if (metadataMap != update) {
            metadataMap = update
            metadataObservers.forEach {
                it.onMetadataUpdated(metadataMap.values.toList())
            }
        }
    }

    private var paused = false

    fun pauseUpdates() {
        Log.e("lp_test", "nativeObserver#pauseUpdates")
        paused = true
    }

    fun resumeUpdates() {
        Log.e("lp_test", "nativeObserver#resumeUpdates")
        paused = false
    }

    fun setProcessedAlternativesMetadata(
        routes: List<NavigationRoute>,
        processedAlternatives: List<RouteAlternative>
    ) {
        val localMetadataMap = mutableMapOf<String, AlternativeRouteMetadata>()
        processedAlternatives.forEach { nativeAlternative ->
            localMetadataMap[nativeAlternative.route.routeId] = nativeAlternative.mapToMetadata(
                routes.find { nativeAlternative.route.routeId == it.id }!!
            )
        }
        notifyMetadataObservers(localMetadataMap)
    }

    private companion object {
        private const val LOG_CATEGORY = "RouteAlternativesController"
    }
}

data class AlternativeRouteMetadata internal constructor(
    val navigationRoute: NavigationRoute,
    val forkIntersectionOfAlternativeRoute: AlternativeRouteIntersection,
    val forkIntersectionOfPrimaryRoute: AlternativeRouteIntersection,
    val infoFromFork: AlternativeRouteInfo,
    val infoFromStartOfPrimary: AlternativeRouteInfo,
)

data class AlternativeRouteIntersection internal constructor(
    val location: Point,
    val geometryIndexInRoute: Int,
    val geometryIndexInLeg: Int,
    val legIndex: Int,
)

data class AlternativeRouteInfo internal constructor(
    val distance: Double,
    val duration: Double,
)

private fun RouteAlternative.mapToMetadata(navigationRoute: NavigationRoute): AlternativeRouteMetadata {
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
