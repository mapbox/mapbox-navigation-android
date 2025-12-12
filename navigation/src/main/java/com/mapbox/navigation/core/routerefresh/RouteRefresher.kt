package com.mapbox.navigation.core.routerefresh

import android.util.Log
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.isExpired
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RoutesRefreshData
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshCallback
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshError
import com.mapbox.navigation.core.internal.utils.CoroutineUtils.withTimeoutOrDefault
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal sealed class RouteRefresherStatus {
    data class Success(val refreshResponse: DataRef) : RouteRefresherStatus()
    object Failure : RouteRefresherStatus()
    object Invalid : RouteRefresherStatus()
    object Invalidated : RouteRefresherStatus()
}

internal data class RouteRefresherResult<out T>(
    val route: NavigationRoute,
    val routeProgressData: T,
    val status: RouteRefresherStatus,
    val wasRouteUpdated: Boolean = status is RouteRefresherStatus.Success,
) {
    fun isSuccess(): Boolean = status is RouteRefresherStatus.Success
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
        return primaryRouteRefresherResult.status == RouteRefresherStatus.Failure ||
            alternativesRouteRefresherResults.any { it.status == RouteRefresherStatus.Failure }
    }

    fun find(routeId: String?): RouteRefresherResult<RouteProgressData?>? {
        if (primaryRouteRefresherResult.route.id == routeId) {
            return primaryRouteRefresherResult
        }
        return alternativesRouteRefresherResults.firstOrNull {
            it.route.id == routeId
        }
    }
}

internal class RouteRefresher(
    private val routesRefreshDataProvider: RoutesRefreshDataProvider,
    private val evRefreshDataProvider: EVRefreshDataProvider,
    private val routeDiffProvider: DirectionsRouteDiffProvider,
    private val routeRefresh: RouteRefresh,
    private val globalScope: CoroutineScope = GlobalScope,
) {

    /**
     * Refreshes routes.
     *
     * @throws IllegalArgumentException when routes are empty
     */
    @Throws(IllegalArgumentException::class)
    suspend fun refresh(
        routes: List<NavigationRoute>,
        routeRefreshTimeout: Long,
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
                    refreshedRoutes[index + 1].status,
                )
            }
        return RoutesRefresherResult(
            RouteRefresherResult(
                primaryRoute,
                routesRefreshData.primaryRouteProgressData,
                refreshedRoutes.first().status,
            ),
            alternativeRoutesRefresherResultData,
        )
    }

    private suspend fun refreshRoutes(
        routesData: RoutesRefreshData,
        timeout: Long,
    ): List<RouteRefresherResult<RouteProgressData?>> {
        return coroutineScope {
            routesData.allRoutesRefreshData.map { routeData ->
                async {
                    val routeProgressData = routeData.second
                    val timeoutDefault = RouteRefresherResult(
                        routeData.first,
                        routeProgressData,
                        RouteRefresherStatus.Failure,
                    )
                    withTimeoutOrDefault(timeout, timeoutDefault) {
                        if (routeProgressData != null) {
                            refreshRoute(routeData.first, routeProgressData)
                        } else {
                            // No RouteProgressData - no refresh. Should not happen in production.
                            Log.w(
                                RouteRefreshLog.LOG_CATEGORY,
                                "Can't refresh route ${routeData.first.id}: " +
                                    "no route progress data for it",
                            )
                            RouteRefresherResult<RouteProgressData?>(
                                routeData.first,
                                routeProgressData,
                                RouteRefresherStatus.Failure,
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
                RouteRefreshLog.LOG_CATEGORY,
            )
            return RouteRefresherResult(
                route,
                routeProgressData,
                RouteRefresherStatus.Invalid,
            )
        }
        if (route.isExpired()) {
            logI(
                "route ${route.id} will not be refreshed because it is invalidated",
                RouteRefreshLog.LOG_CATEGORY,
            )
            return RouteRefresherResult(
                route,
                routeProgressData,
                RouteRefresherStatus.Invalidated,
            )
        }
        val routeRefreshRequestData = RouteRefreshRequestData(
            routeProgressData.legIndex,
            routeProgressData.routeGeometryIndex,
            routeProgressData.legGeometryIndex,
            evRefreshDataProvider.get(route.routeOptions),
        )
        return when (val result = requestRouteRefresh(route, routeRefreshRequestData)) {
            is RouteRefreshResult.Fail -> {
                logE(
                    "Route refresh error: ${result.error.message} " +
                        "throwable=${result.error.throwable}",
                    RouteRefreshLog.LOG_CATEGORY,
                )
                val status = if (result.error.refreshTtl == 0) {
                    RouteRefresherStatus.Invalidated
                } else {
                    RouteRefresherStatus.Failure
                }
                RouteRefresherResult(route, routeProgressData, status)
            }
            is RouteRefreshResult.Success -> {
                logI(
                    "Received refreshed route ${result.route.id}",
                    RouteRefreshLog.LOG_CATEGORY,
                )
                logRoutesDiff(
                    newRoute = result.route,
                    oldRoute = route,
                    currentLegIndex = routeRefreshRequestData.legIndex,
                )
                RouteRefresherResult(
                    result.route,
                    routeProgressData,
                    RouteRefresherStatus.Success(result.refreshResponse),
                )
            }
        }
    }

    private suspend fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
    ): RouteRefreshResult =
        suspendCancellableCoroutine { continuation ->
            val requestId = routeRefresh.requestRouteRefresh(
                route,
                routeRefreshRequestData,
                object : NavigationRouterRefreshCallback {
                    override fun onRefreshReady(route: NavigationRoute, refreshResponse: DataRef) {
                        continuation.resume(RouteRefreshResult.Success(route, refreshResponse))
                    }

                    override fun onFailure(error: NavigationRouterRefreshError) {
                        // we might get this callback when the request is cancelled
                        // and the continuation is cancelled with it
                        if (continuation.isActive) {
                            continuation.resume(RouteRefreshResult.Fail(error))
                        }
                    }
                },
            )
            continuation.invokeOnCancellation {
                logI(
                    "Route refresh for route ${route.id} was cancelled after timeout",
                    RouteRefreshLog.LOG_CATEGORY,
                )
                globalScope.launch(Dispatchers.Main.immediate) {
                    routeRefresh.cancelRouteRefreshRequest(requestId)
                }
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
                RouteRefreshLog.LOG_CATEGORY,
            )
        } else {
            for (diff in routeDiffs) {
                logI(diff, RouteRefreshLog.LOG_CATEGORY)
            }
        }
    }

    private sealed class RouteRefreshResult {
        data class Success(
            val route: NavigationRoute,
            val refreshResponse: DataRef,
        ) : RouteRefreshResult()
        data class Fail(val error: NavigationRouterRefreshError) : RouteRefreshResult()
    }
}
