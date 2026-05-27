package com.mapbox.navigation.core.routealternatives.internal

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.routealternatives.mapToMetadata
import com.mapbox.navigator.RouteParser

/**
 * Computes [AlternativeRouteMetadata] for a list of routes.
 *
 * Returns [Result.success] with the list (possibly empty for fewer than 2 routes),
 * or [Result.failure] if the native call throws.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@WorkerThread
fun calculateAlternativesMetadata(
    routes: List<NavigationRoute>,
): Result<List<AlternativeRouteMetadata>> {
    if (routes.size < 2) return Result.success(emptyList())
    return PerformanceTracker.trackPerformanceSync("calculateAlternativesMetadata") {
        runCatching {
            val routesData = RouteParser.createRoutesData(
                routes.first().nativeRoute(),
                routes.drop(1).map { it.nativeRoute() },
            )
            routesData.alternativeRoutes().mapNotNull { nativeAlt ->
                routes.firstOrNull { it.id == nativeAlt.route.routeId }
                    ?.let { nativeAlt.mapToMetadata(it) }
            }
        }
    }
}
