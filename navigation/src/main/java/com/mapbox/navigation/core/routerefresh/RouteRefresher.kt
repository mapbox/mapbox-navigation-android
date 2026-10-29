package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.DataRef
import com.mapbox.common.dispatchers.SdkDispatchers
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
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal sealed class RouteRefresherStatus {
    data class Success(
        val refreshResponse: DataRef,
    ) : RouteRefresherStatus()
    object Failure : RouteRefresherStatus()
    object Invalid : RouteRefresherStatus()
    object Invalidated : RouteRefresherStatus()

    /** Permanent: this route cannot be refreshed (e.g. OfflineRefresh). Do not retry. */
    object Unsupported : RouteRefresherStatus()
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

    fun allUnsupported(): Boolean {
        return primaryRouteRefresherResult.status == RouteRefresherStatus.Unsupported &&
            alternativesRouteRefresherResults.all {
                it.status == RouteRefresherStatus.Unsupported
            }
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
    private val routeRefresh: RouteRefresh,
    private val globalScope: CoroutineScope = GlobalScope,
) {

    // Routes the router answered can never be refreshed. They are not requested again.
    // A plain set: every refresh coroutine runs on the single main dispatcher
    // (RouteRefreshControllerProvider gets SdkDispatchers.Main), so two refreshes never touch it
    // at the same time, even when a cancelled one is still finishing.
    private val unrefreshableRouteIds = mutableSetOf<String>()

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
        unrefreshableRouteIds.retainAll(routes.map { it.id }.toSet())
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
                            logW(RouteRefreshLog.LOG_CATEGORY) {
                                "Can't refresh route ${routeData.first.id}: " +
                                    "no route progress data for it"
                            }
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
        if (route.id in unrefreshableRouteIds) {
            logI(
                "route ${route.id} is not refreshed again: the router can't refresh it",
                RouteRefreshLog.LOG_CATEGORY,
            )
            return RouteRefresherResult(route, routeProgressData, RouteRefresherStatus.Unsupported)
        }
        val validationResult =
            RouteRefreshValidator.validateRoute(route, routeRefresh::supportsOnDeviceRefresh)
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
                val status = when {
                    result.error.refreshTtl == 0 -> RouteRefresherStatus.Invalidated
                    // Not a Failure: a failure is retried, and this route never refreshes.
                    result.error.permanent -> {
                        unrefreshableRouteIds.add(route.id)
                        RouteRefresherStatus.Unsupported
                    }
                    else -> RouteRefresherStatus.Failure
                }
                RouteRefresherResult(route, routeProgressData, status)
            }
            is RouteRefreshResult.Success -> {
                logI(
                    "Received refreshed route ${result.route.id}",
                    RouteRefreshLog.LOG_CATEGORY,
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
                globalScope.launch(SdkDispatchers.Main.immediate) {
                    routeRefresh.cancelRouteRefreshRequest(requestId)
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
