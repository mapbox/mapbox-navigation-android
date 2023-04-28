package com.mapbox.navigation.core.routerefresh

import android.util.Log
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.core.RoutesRefreshData
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal data class RouteRefresherResult(
    val success: Boolean,
    val refreshedRoutesData: RoutesRefreshData,
)

internal class RouteRefresher(
    private val routesRefreshDataProvider: RoutesRefreshDataProvider,
    private val evRefreshDataProvider: EVRefreshDataProvider,
    private val routeDiffProvider: DirectionsRouteDiffProvider,
    private val routeRefresh: RouteRefresh,
) {

    /**
     * Refreshes routes.
     *
     * @throws IllegalArgumentException when routes are empty
     */
    @Throws(IllegalArgumentException::class)
    suspend fun refresh(
        routes: List<NavigationRoute>,
        routeRefreshTimeout: Long
    ): RouteRefresherResult {
        val routesRefreshData = routesRefreshDataProvider.getRoutesRefreshData(routes)
        val refreshedRoutes = refreshRoutesOrNull(routesRefreshData, routeRefreshTimeout)
        return if (refreshedRoutes.any { it != null }) {
            val primaryRoute = refreshedRoutes.first() ?: routes.first()
            val alternativeRoutesProgressData =
                routesRefreshData.alternativeRoutesProgressData.mapIndexed { index, pair ->
                    (refreshedRoutes[index + 1] ?: routes[index + 1]) to pair.second
                }
            RouteRefresherResult(
                success = true,
                RoutesRefreshData(
                    primaryRoute,
                    routesRefreshData.primaryRouteProgressData,
                    alternativeRoutesProgressData
                )
            )
        } else {
            RouteRefresherResult(
                success = false,
                routesRefreshData
            )
        }
    }

    private suspend fun refreshRoutesOrNull(
        routesData: RoutesRefreshData,
        timeout: Long
    ): List<NavigationRoute?> {
        return coroutineScope {
            routesData.allRoutesRefreshData.map { routeData ->
                async {
                    withTimeoutOrNull(timeout) {
                        val routeProgressData = routeData.second
                        if (routeProgressData != null) {
                            refreshRouteOrNull(routeData.first, routeProgressData)
                        } else {
                            // No RouteProgressData - no refresh. Should not happen in production.
                            Log.w(
                                RouteRefreshLog.LOG_CATEGORY,
                                "Can't refresh route ${routeData.first.id}: " +
                                    "no route progress data for it"
                            )
                            null
                        }
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
            evRefreshDataProvider.get(route.routeOptions)
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

    private sealed class RouteRefreshResult {
        data class Success(val route: NavigationRoute) : RouteRefreshResult()
        data class Fail(val error: NavigationRouterRefreshError) : RouteRefreshResult()
    }
}
