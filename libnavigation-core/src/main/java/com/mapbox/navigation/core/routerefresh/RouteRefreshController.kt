package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.route.refreshRoute
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import kotlin.coroutines.resume

private const val FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA = 3

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 */
internal class RouteRefreshController(
    private val routeRefreshOptions: RouteRefreshOptions,
    private val directionsSession: DirectionsSession,
    private val currentLegIndexProvider: () -> Int,
    private val routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
    private val localDateProvider: () -> Date
) {

    internal companion object {
        internal const val LOG_CATEGORY = "RouteRefreshController"
    }

    suspend fun refresh(routes: List<NavigationRoute>): List<NavigationRoute> {
        return if (routes.isNotEmpty()) {
            val routeToRefresh = routes.first()
            when (val validationResult = validateRoute(routeToRefresh)) {
                RouteValidationResult.Valid -> {
                    val result = routes.toMutableList()
                    result[0] = tryRefreshingUntilRouteChanges(routeToRefresh)
                    result
                }
                is RouteValidationResult.Invalid -> waitForever(validationResult.reason)
            }
        } else waitForever("routes are empty")
    }

    private suspend fun tryRefreshingUntilRouteChanges(
        initialRoute: NavigationRoute
    ): NavigationRoute {
        while (true) {
            val refreshed = refreshRoute(initialRoute)
            if (refreshed != initialRoute) {
                return refreshed
            }
        }
    }

    private suspend fun refreshRoute(
        route: NavigationRoute
    ): NavigationRoute = coroutineScope {
        val routeLegs = route.directionsRoute.legs()
        require(routeLegs != null) { "Can't refresh route without legs" }

        var timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
        repeat(FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA) {
            timeUntilNextAttempt.await()
            timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
            val refreshedRoute = withTimeoutOrNull(routeRefreshOptions.intervalMillis) {
                refreshRouteOrNull(route)
            }
            if (refreshedRoute != null) {
                timeUntilNextAttempt.cancel()
                return@coroutineScope refreshedRoute
            }
        }
        timeUntilNextAttempt.cancel()
        removeExpiringDataFromRoute(route, routeLegs)
    }

    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        routeLegs: List<RouteLeg>
    ): NavigationRoute {
        val currentLegIndex = currentLegIndexProvider()
        return route.refreshRoute(
            currentLegIndex,
            routeLegs.mapIndexed { index, leg ->
                if (index >= currentLegIndex) {
                    leg.annotation()
                        ?.toBuilder()
                        ?.congestion(leg.annotation()?.congestion()?.map { "unknown" })
                        ?.congestionNumeric(leg.annotation()?.congestionNumeric()?.map { null })
                        ?.build()
                } else {
                    leg.annotation()
                }
            },
            routeLegs.mapIndexed { index, it ->
                if (index >= currentLegIndex) {
                    it.incidents()?.filter {
                        val parsed = parseISO8601DateToLocalTimeOrNull(it.endTime())
                            ?: return@filter false
                        val currentDate = localDateProvider()
                        parsed > currentDate
                    }
                } else {
                    it.incidents()
                }
            }
        )
    }

    private suspend fun refreshRouteOrNull(
        route: NavigationRoute
    ): NavigationRoute? {
        val legIndex = currentLegIndexProvider()
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
                logRoutesDIff(
                    newRoute = result.route,
                    oldRoute = route,
                    currentLegIndex = legIndex
                )
                result.route
            }
        }
    }

    private fun logRoutesDIff(
        newRoute: NavigationRoute,
        oldRoute: NavigationRoute,
        currentLegIndex: Int,
    ) {
        logI("Successful route refresh", LOG_CATEGORY)
        val routeDiffs = routeDiffProvider.buildRouteDiffs(
            oldRoute,
            newRoute,
            currentLegIndex,
        )
        if (routeDiffs.isEmpty()) {
            logI("No changes to route annotations", LOG_CATEGORY)
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
            val requestId = directionsSession.requestRouteRefresh(
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
                directionsSession.cancelRouteRefreshRequest(requestId)
            }
        }

    private suspend fun waitForever(message: String): List<NavigationRoute> {
        logI("Route won't be refreshed because $message", LOG_CATEGORY)
        return CompletableDeferred<List<NavigationRoute>>().await()
    }

    private fun validateRoute(route: NavigationRoute): RouteValidationResult = when {
        route.routeOptions.enableRefresh() != true ->
            RouteValidationResult.Invalid("RouteOptions#enableRefresh is false")
        route.directionsRoute.requestUuid()?.isNotBlank() != true ->
            RouteValidationResult.Invalid(
                "DirectionsRoute#requestUuid is ${route.directionsRoute.requestUuid()}"
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
