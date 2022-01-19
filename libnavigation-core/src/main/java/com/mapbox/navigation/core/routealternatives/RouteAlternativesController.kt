package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.utils.parseNativeDirectionsAlternative
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.mapToSdkRouteOrigin
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteAlternative
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                logE(TAG, Message("Error: ${error.message}"))
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
                    // NN should wrap alternatives in NavigationRoute
                    // refs https://github.com/mapbox/mapbox-navigation-native/issues/5142
                    val options = routeProgress.navigationRoute.routeOptions
                    processRouteAlternatives(
                        options,
                        value
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
    }

    private val nativeObserver = object : com.mapbox.navigator.RouteAlternativesObserver {
        override fun onRouteAlternativesChanged(
            routeAlternatives: List<RouteAlternative>,
            removed: List<RouteAlternative>
        ): List<Int> {
            val routeProgress = tripSession.getRouteProgress()
                ?: return emptyList()

            // NN should expose origin of a failed alternatives request and the used URL,
            // refs https://github.com/mapbox/mapbox-navigation-native/issues/5401
            // and https://github.com/mapbox/mapbox-navigation-native/issues/5402
            processRouteAlternatives(
                routeProgress.navigationRoute.routeOptions,
                routeAlternatives
            ) { alternatives, origin ->
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
        routeOptions: RouteOptions,
        nativeAlternatives: List<RouteAlternative>,
        block: (List<NavigationRoute>, RouterOrigin) -> Unit,
    ) {
        val alternatives: List<NavigationRoute> = runBlocking {
            nativeAlternatives.mapIndexedNotNull { index, routeAlternative ->
                val expected = parseNativeDirectionsAlternative(
                    ThreadController.IODispatcher,
                    routeAlternative.route.response,
                    routeOptions
                )
                if (expected.isValue) {
                    expected.value
                } else {
                    logE(
                        TAG,
                        Message(
                            """
                                    |unable to parse alternative at index $index;
                                    |failure for response: ${routeAlternative.route.response}
                                """.trimMargin()
                        ),
                        expected.error
                    )
                    null
                }
            }
        }
        logI(TAG, Message("${alternatives.size} alternatives available"))

        mainJobControl.scope.launch {
            val origin = nativeAlternatives.find {
                // looking for the first new route,
                // assuming all new routes come from the same request
                it.isNew
            }?.route?.routerOrigin?.mapToSdkRouteOrigin() ?: lastUpdateOrigin
            block(alternatives, origin)
            lastUpdateOrigin = origin
        }
    }

    private companion object {
        private val TAG = Tag("MbxRouteAlternativesController")
    }
}
