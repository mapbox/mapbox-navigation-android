package com.mapbox.navigation.core

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.None
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.models.nn.RouteInterfacesParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.NavigatorOperationsDelegate
import com.mapbox.navigator.NavigatorOperationsStartActiveGuidanceCallback
import com.mapbox.navigator.NavigatorOperationsStartActiveGuidanceResult
import com.mapbox.navigator.NavigatorOperationsSwitchToAlternativeRouteCallback
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Implementation of the native [NavigatorOperationsDelegate] that routes route-mutating operations
 * requested by SDK components (e.g. C++ presenters) back through [MapboxNavigation].
 *
 * Applying such changes directly to the shared native `Navigator` would bypass [MapboxNavigation],
 * leaving its route state (`DirectionsSession` cache and `RoutesObserver`s) stale. Delegating to the
 * public route-setting entry points keeps [MapboxNavigation] the single writer of route state.
 *
 * @param mapboxNavigation the navigation owner whose route-setting entry points are invoked.
 * @param routesParser parses each incoming [RouteInterface] into a [NavigationRoute] without a
 *  redundant native re-parse; [RouteInterfacesParser.parseRoutes] runs unconditionally, unlike
 *  [RouteInterfacesParser.parserContinuousAlternatives] which can skip under contention.
 * @param scope coroutine scope used for the suspending route parsing; must be the main scope used by
 *  [MapboxNavigation] so route operations stay serialized with the rest of its route handling.
 */
internal class MapboxNavigatorOperationsDelegate(
    private val mapboxNavigation: MapboxNavigation,
    private val routesParser: RouteInterfacesParser,
    private val scope: CoroutineScope,
) : NavigatorOperationsDelegate {

    override fun startActiveGuidance(
        routes: List<RouteInterface>,
        initialLegIndex: Int,
        callback: NavigatorOperationsStartActiveGuidanceCallback,
    ) {
        if (routes.isEmpty()) {
            callback.run(ExpectedFactory.createError("No routes provided"))
            return
        }
        scope.launch {
            val navigationRoutes = parseRoutes(routes)
            if (navigationRoutes == null) {
                callback.run(ExpectedFactory.createError("Failed to parse routes"))
                return@launch
            }
            mapboxNavigation.setNavigationRoutes(navigationRoutes, initialLegIndex) { result ->
                callback.run(
                    result.fold(
                        { error -> ExpectedFactory.createError(error.message) },
                        {
                            ExpectedFactory.createValue(
                                NavigatorOperationsStartActiveGuidanceResult(
                                    mapboxNavigation.getNavigationRoutes().map { it.id },
                                ),
                            )
                        },
                    ),
                )
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun switchToAlternativeRoute(
        routeId: String,
        callback: NavigatorOperationsSwitchToAlternativeRouteCallback,
    ) {
        val alternativeRoute =
            mapboxNavigation.getNavigationRoutes().firstOrNull { it.id == routeId }
        if (alternativeRoute == null) {
            callback.run(
                ExpectedFactory.createError(
                    "Alternative route with id $routeId is not among currently tracked routes",
                ),
            )
            return
        }
        mapboxNavigation.switchToAlternativeRoute(alternativeRoute) { result ->
            callback.run(
                result.fold(
                    { error -> ExpectedFactory.createError(error.message) },
                    { ExpectedFactory.createValue(None.getInstance()) },
                ),
            )
        }
    }

    private suspend fun parseRoutes(routes: List<RouteInterface>): List<NavigationRoute>? {
        return routesParser.parseRoutes(routes).fold(
            { it.routes },
            {
                logE(LOG_CATEGORY) { "Unable to parse routes: ${it.message}" }
                null
            },
        )
    }

    private companion object {
        private const val LOG_CATEGORY = "MapboxNavigatorOperationsDelegate"
    }
}
