package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.route.updateDirectionsRouteOnly
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import kotlin.coroutines.resume

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 */
internal class RouteRefreshController(
    private val routeRefreshOptions: RouteRefreshOptions,
    private val routeRefresh: RouteRefresh,
    private val routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
    private val localDateProvider: () -> Date
) {

    internal companion object {
        internal const val LOG_CATEGORY = "RouteRefreshController"
        private const val FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA = 3
    }

    suspend fun refresh(routes: List<NavigationRoute>, legIndex: Int): List<NavigationRoute> {
        return if (routes.isNotEmpty()) {
            val routesValidationResults = routes.map { validateRoute(it) }
            if (routesValidationResults.any { it is RouteValidationResult.Valid }) {
                tryRefreshingRoutesUntilRouteChanges(routes, legIndex)
            } else {
                val message = joinValidationErrorMessages(routesValidationResults, routes)
                waitForever("No routes which could be refreshed. $message")
            }
        } else waitForever("routes are empty")
    }

    private fun joinValidationErrorMessages(
        routeValidation: List<RouteValidationResult>,
        routes: List<NavigationRoute>
    ): String = routeValidation.filterIsInstance<RouteValidationResult.Invalid>()
        .mapIndexed { index, validation -> "${routes[index].id} ${validation.reason}" }
        .joinToString(separator = ". ")

    private suspend fun tryRefreshingRoutesUntilRouteChanges(
        initialRoutes: List<NavigationRoute>,
        legIndex: Int,
    ): List<NavigationRoute> {
        while (true) {
            val refreshed = refreshRoutesWithRetry(initialRoutes, legIndex)
            if (refreshed != initialRoutes) {
                return refreshed
            }
        }
    }

    private suspend fun refreshRoutesWithRetry(
        routes: List<NavigationRoute>,
        legIndex: Int,
    ): List<NavigationRoute> = coroutineScope {
        var timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
        try {
            repeat(FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA) {
                timeUntilNextAttempt.await()
                timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
                val refreshedRoutes = refreshRoutesOrNull(routes, legIndex)
                if (refreshedRoutes.any { it != null }) {
                    return@coroutineScope refreshedRoutes.mapIndexed { index, navigationRoute ->
                        navigationRoute ?: routes[index]
                    }
                }
            }
        } finally {
            timeUntilNextAttempt.cancel() // otherwise current coroutine will wait for its child
        }
        routes.map { removeExpiringDataFromRoute(it, legIndex) }
    }

    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        currentLegIndex: Int,
    ): NavigationRoute {
        val routeLegs = route.directionsRoute.legs()
        return route.updateDirectionsRouteOnly {
            toBuilder().legs(
                routeLegs?.mapIndexed { legIndex, leg ->
                    val legHasAlreadyBeenPassed = legIndex < currentLegIndex
                    if (legHasAlreadyBeenPassed) {
                        leg
                    } else removeExpiredDataFromLeg(leg)
                }
            ).build()
        }
    }

    private fun removeExpiredDataFromLeg(leg: RouteLeg) =
        leg.toBuilder()
            .annotation(
                leg.annotation()?.toBuilder()
                    ?.congestion(leg.annotation()?.congestion()?.map { "unknown" })
                    ?.congestionNumeric(
                        leg.annotation()?.congestionNumeric()?.map { null }
                    )
                    ?.build()
            )
            .incidents(
                leg.incidents()?.filter {
                    val parsed = parseISO8601DateToLocalTimeOrNull(it.endTime())
                        ?: return@filter true
                    val currentDate = localDateProvider()
                    parsed > currentDate
                }
            )
            .build()

    private suspend fun refreshRouteOrNull(
        route: NavigationRoute,
        legIndex: Int
    ): NavigationRoute? {
        val validationResult = validateRoute(route)
        if (validationResult is RouteValidationResult.Invalid) {
            logI("route ${route.id} can't be refreshed because ${validationResult.reason}")
            return null
        }
        return when (val result = requestRouteRefresh(route, legIndex)) {
            is RouteRefreshResult.Fail -> {
                logE(
                    "Route refresh error: ${result.error.message} " +
                        "throwable=${result.error.throwable}",
                    LOG_CATEGORY
                )
                null
            }
            is RouteRefreshResult.Success -> {
                logI("Received refreshed route ${result.route.id}", LOG_CATEGORY)
                logRoutesDiff(
                    newRoute = result.route,
                    oldRoute = route,
                    currentLegIndex = legIndex
                )
                result.route
            }
        }
    }

    private suspend fun refreshRoutesOrNull(
        routes: List<NavigationRoute>,
        legIndex: Int,
    ): List<NavigationRoute?> {
        return coroutineScope {
            routes.map { route ->
                async {
                    withTimeoutOrNull(routeRefreshOptions.intervalMillis) {
                        refreshRouteOrNull(route, legIndex)
                    }
                }
            }.awaitAll()
        }
    }

    private fun logRoutesDiff(
        newRoute: NavigationRoute,
        oldRoute: NavigationRoute,
        currentLegIndex: Int,
    ) {
        val routeDiffs = routeDiffProvider.buildRouteDiffs(
            oldRoute,
            newRoute,
            currentLegIndex,
        )
        if (routeDiffs.isEmpty()) {
            logI("No changes in annotations for route ${newRoute.id}", LOG_CATEGORY)
        } else {
            for (diff in routeDiffs) {
                logI(diff, LOG_CATEGORY)
            }
        }
    }

    private suspend fun requestRouteRefresh(
        route: NavigationRoute,
        legIndex: Int
    ): RouteRefreshResult =
        suspendCancellableCoroutine { continuation ->
            val requestId = routeRefresh.requestRouteRefresh(
                route,
                legIndex,
                object : NavigationRouterRefreshCallback {
                    override fun onRefreshReady(route: NavigationRoute) {
                        continuation.resume(RouteRefreshResult.Success(route))
                    }

                    override fun onFailure(error: NavigationRouterRefreshError) {
                        continuation.resume(RouteRefreshResult.Fail(error))
                    }
                }
            )
            continuation.invokeOnCancellation {
                routeRefresh.cancelRouteRefreshRequest(requestId)
            }
        }

    private suspend fun <T> waitForever(message: String): T {
        logI("Route won't be refreshed because $message", LOG_CATEGORY)
        return CompletableDeferred<T>().await()
    }

    private fun validateRoute(route: NavigationRoute): RouteValidationResult = when {
        route.routeOptions.enableRefresh() != true ->
            RouteValidationResult.Invalid("RouteOptions#enableRefresh is false")
        route.directionsRoute.requestUuid()?.isNotBlank() != true ->
            RouteValidationResult.Invalid(
                "DirectionsRoute#requestUuid is blank. " +
                    "This can be caused by a route being generated by " +
                    "an Onboard router (in offline mode). " +
                    "Make sure to switch to an Offboard route when possible, " +
                    "only Offboard routes support the refresh feature."
            )
        else -> RouteValidationResult.Valid
    }

    private sealed class RouteValidationResult {
        object Valid : RouteValidationResult()
        data class Invalid(val reason: String) : RouteValidationResult()
    }

    private sealed class RouteRefreshResult {
        data class Success(val route: NavigationRoute) : RouteRefreshResult()
        data class Fail(val error: NavigationRouterRefreshError) : RouteRefreshResult()
    }
}
