package com.mapbox.navigation.core.routerefresh

import android.util.Log
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.RouteExpirationHandler
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.core.RoutesRefreshData
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.utils.CoroutineUtils.withTimeoutOrDefault
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal enum class RouteRefresherStatus {
    SUCCESS, FAILURE, INVALID, INVALIDATED
}

internal data class RouteRefresherResult<T>(
    val route: NavigationRoute,
    val routeProgressData: T,
    val status: RouteRefresherStatus,
) {

    fun isSuccess(): Boolean = status == RouteRefresherStatus.SUCCESS
}

internal data class RoutesRefresherResult(
    val primaryRouteRefresherResult: RouteRefresherResult<RouteProgressData>,
    val alternativesRouteRefresherResults: List<RouteRefresherResult<RouteProgressData?>>,
) {
    fun anySuccess(): Boolean {
        return primaryRouteRefresherResult.isSuccess() ||
            alternativesRouteRefresherResults.any { it.isSuccess() }
    }

    fun anyRequestFailed(): Boolean {
        return primaryRouteRefresherResult.status == RouteRefresherStatus.FAILURE ||
            alternativesRouteRefresherResults.any { it.status == RouteRefresherStatus.FAILURE }
    }
}

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
    ): RoutesRefresherResult {
        val routesRefreshData = routesRefreshDataProvider.getRoutesRefreshData(routes)
        val refreshedRoutes = refreshRoutes(routesRefreshData, routeRefreshTimeout)
        val primaryRoute = if (refreshedRoutes.first().isSuccess()) {
            refreshedRoutes.first().route
        } else {
            routes.first()
        }
        val alternativeRoutesRefresherResultData =
            routesRefreshData.alternativeRoutesProgressData.mapIndexed { index, pair ->
                RouteRefresherResult(
                    if (refreshedRoutes[index + 1].isSuccess()) {
                        refreshedRoutes[index + 1].route
                    } else {
                        routes[index + 1]
                    },
                    pair.second,
                    refreshedRoutes[index + 1].status
                )
            }
        return RoutesRefresherResult(
            RouteRefresherResult(
                primaryRoute,
                routesRefreshData.primaryRouteProgressData,
                refreshedRoutes.first().status
            ),
            alternativeRoutesRefresherResultData
        )
    }

    private suspend fun refreshRoutes(
        routesData: RoutesRefreshData,
        timeout: Long
    ): List<RouteRefresherResult<RouteProgressData?>> {
        return coroutineScope {
            routesData.allRoutesRefreshData.map { routeData ->
                async {
                    val routeProgressData = routeData.second
                    val timeoutDefault = RouteRefresherResult(
                        routeData.first,
                        routeProgressData,
                        RouteRefresherStatus.FAILURE
                    )
                    withTimeoutOrDefault(timeout, timeoutDefault) {
                        if (routeProgressData != null) {
                            refreshRoute(routeData.first, routeProgressData)
                        } else {
                            // No RouteProgressData - no refresh. Should not happen in production.
                            Log.w(
                                RouteRefreshLog.LOG_CATEGORY,
                                "Can't refresh route ${routeData.first.id}: " +
                                    "no route progress data for it"
                            )
                            RouteRefresherResult<RouteProgressData?>(
                                routeData.first,
                                routeProgressData,
                                RouteRefresherStatus.FAILURE
                            )
                        }
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun refreshRoute(
        route: NavigationRoute,
        routeProgressData: RouteProgressData,
    ): RouteRefresherResult<RouteProgressData?> {
        val validationResult = RouteRefreshValidator.validateRoute(route)
        if (validationResult is RouteRefreshValidator.RouteValidationResult.Invalid) {
            logI(
                "route ${route.id} can't be refreshed because ${validationResult.reason}",
                RouteRefreshLog.LOG_CATEGORY
            )
            return RouteRefresherResult(route, routeProgressData, RouteRefresherStatus.INVALID)
        }
        if (RouteExpirationHandler.isRouteExpired(route)) {
            logI(
                "route ${route.id} will not be refreshed because it is invalidated",
                RouteRefreshLog.LOG_CATEGORY
            )
            return RouteRefresherResult(route, routeProgressData, RouteRefresherStatus.INVALIDATED)
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
                val status = if (result.error.refreshTtl == 0) {
                    RouteRefresherStatus.INVALIDATED
                } else {
                    RouteRefresherStatus.FAILURE
                }
                RouteRefresherResult(route, routeProgressData, status)
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
                RouteRefresherResult(result.route, routeProgressData, RouteRefresherStatus.SUCCESS)
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
