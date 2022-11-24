package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.core.RouteProgressDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import kotlin.coroutines.resume

internal data class RouteRefresherResult(
    val success: Boolean,
    val refreshedRoutes: List<NavigationRoute>,
    val routeProgressData: RouteProgressData
)

internal class RouteRefresher(
    private val routeProgressDataProvider: RouteProgressDataProvider,
    private val evDataHolder: EVDynamicDataHolder,
    private val routeDiffProvider: DirectionsRouteDiffProvider,
    private val routeRefresh: RouteRefresh,
    private val localDateProvider: () -> Date,
) {

    suspend fun refresh(
        routes: List<NavigationRoute>,
        routeRefreshTimeout: Long
    ): RouteRefresherResult {
        val routeProgressData = routeProgressDataProvider.getRouteRefreshRequestDataOrWait()
        val refreshedRoutes = refreshRoutesOrNull(routes, routeProgressData, routeRefreshTimeout)
        return if (refreshedRoutes.any { it != null }) {
            RouteRefresherResult(
                success = true,
                refreshedRoutes.mapIndexed { index, navigationRoute ->
                    navigationRoute ?: routes[index]
                },
                routeProgressData
            )
        } else {
            RouteRefresherResult(
                success = false,
                routes.map { removeExpiringDataFromRoute(it, routeProgressData.legIndex) },
                routeProgressData
            )
        }
    }

    private suspend fun refreshRoutesOrNull(
        routes: List<NavigationRoute>,
        routeProgressData: RouteProgressData,
        timeout: Long
    ): List<NavigationRoute?> {
        return coroutineScope {
            routes.map { route ->
                async {
                    withTimeoutOrNull(timeout) {
                        refreshRouteOrNull(route, routeProgressData)
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun refreshRouteOrNull(
        route: NavigationRoute,
        routeProgressData: RouteProgressData,
    ): NavigationRoute? {
        val validationResult = RouteRefreshValidator.validateRoute(route)
        if (validationResult is RouteRefreshValidator.RouteValidationResult.Invalid) {
            logI(
                "route ${route.id} can't be refreshed because ${validationResult.reason}",
                RouteRefreshLog.LOG_CATEGORY
            )
            return null
        }
        val routeRefreshRequestData = RouteRefreshRequestData(
            routeProgressData.legIndex,
            routeProgressData.routeGeometryIndex,
            routeProgressData.legGeometryIndex,
            evDataHolder.currentData(route.routeOptions.unrecognizedJsonProperties ?: emptyMap())
        )
        return when (val result = requestRouteRefresh(route, routeRefreshRequestData)) {
            is RouteRefreshResult.Fail -> {
                logE(
                    "Route refresh error: ${result.error.message} " +
                        "throwable=${result.error.throwable}",
                    RouteRefreshLog.LOG_CATEGORY
                )
                null
            }
            is RouteRefreshResult.Success -> {
                logI(
                    "Received refreshed route ${result.route.id}",
                    RouteRefreshLog.LOG_CATEGORY
                )
                logRoutesDiff(
                    newRoute = result.route,
                    oldRoute = route,
                    currentLegIndex = routeRefreshRequestData.legIndex
                )
                result.route
            }
        }
    }

    private suspend fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData
    ): RouteRefreshResult =
        suspendCancellableCoroutine { continuation ->
            val requestId = routeRefresh.requestRouteRefresh(
                route,
                routeRefreshRequestData,
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
                logI(
                    "Route refresh for route ${route.id} was cancelled after timeout",
                    RouteRefreshLog.LOG_CATEGORY
                )
                routeRefresh.cancelRouteRefreshRequest(requestId)
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
            logI(
                "No changes in annotations for route ${newRoute.id}",
                RouteRefreshLog.LOG_CATEGORY
            )
        } else {
            for (diff in routeDiffs) {
                logI(diff, RouteRefreshLog.LOG_CATEGORY)
            }
        }
    }

    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        currentLegIndex: Int,
    ): NavigationRoute {
        val routeLegs = route.directionsRoute.legs()
        val directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute = {
            toBuilder().legs(
                routeLegs?.mapIndexed { legIndex, leg ->
                    val legHasAlreadyBeenPassed = legIndex < currentLegIndex
                    if (legHasAlreadyBeenPassed) {
                        leg
                    } else {
                        removeExpiredDataFromLeg(leg)
                    }
                }
            ).build()
        }
        return route.update(
            directionsRouteBlock = directionsRouteBlock,
            directionsResponseBlock = { this }
        )
    }

    private fun removeExpiredDataFromLeg(leg: RouteLeg): RouteLeg {
        val oldAnnotation = leg.annotation()
        return leg.toBuilder()
            .annotation(
                oldAnnotation?.let { nonNullOldAnnotation ->
                    nonNullOldAnnotation.toBuilder()
                        .congestion(nonNullOldAnnotation.congestion()?.map { "unknown" })
                        .congestionNumeric(nonNullOldAnnotation.congestionNumeric()?.map { null })
                        .build()
                }
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
    }

    private sealed class RouteRefreshResult {
        data class Success(val route: NavigationRoute) : RouteRefreshResult()
        data class Fail(val error: NavigationRouterRefreshError) : RouteRefreshResult()
    }
}
