package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.utils.routeRefresh.RouteRefreshUtils
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ImmediateRouteRefreshController(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val stateHolder: RouteRefreshStateHolder,
    private val scope: CoroutineScope,
    private val listener: RouteRefresherListener,
    private val attemptListener: RoutesRefreshAttemptListener,
    private val routeRefreshUtils: RouteRefreshUtils,
    private val currentPrimaryRouteIdProvider: () -> String?,
) {

    @Throws(IllegalArgumentException::class)
    fun requestRoutesRefresh(
        routes: List<NavigationRoute>,
        callback: (RoutesRefresherExecutorResult) -> Unit,
        onCancellation: () -> Unit = {},
    ): Job {
        if (routes.isEmpty()) {
            throw IllegalArgumentException("Routes to refresh should not be empty")
        }
        return scope.launch {
            val result = try {
                routeRefresherExecutor.executeRoutesRefresh(
                    routes,
                    startCallback = { stateHolder.onStarted() },
                )
            } catch (ex: CancellationException) {
                stateHolder.onCancel()
                onCancellation()
                throw ex
            }

            callback(result)
            when (result) {
                is RoutesRefresherExecutorResult.ReplacedByNewer -> {
                    logW(
                        "Route refresh on-demand error: " +
                            "request is skipped as a newer one is available",
                        RouteRefreshLog.LOG_CATEGORY,
                    )
                }

                is RoutesRefresherExecutorResult.Finished -> {
                    attemptListener.onRoutesRefreshAttemptFinished(result.value)
                    if (result.value.anySuccess()) {
                        stateHolder.onSuccess()
                    } else {
                        stateHolder.onFailure(null)
                    }

                    val currentPrimaryId = currentPrimaryRouteIdProvider()
                    val refreshedPrimaryId = if (result.value.anySuccess()) {
                        result.value.primaryRouteRefresherResult.route.id
                    } else {
                        null
                    }

                    if (routeRefreshUtils.isResultStale(currentPrimaryId, refreshedPrimaryId)) {
                        logW(
                            "Skipping stale immediate refresh result, as primary route" +
                                " changed since refresh started. Refreshed " +
                                "primary=$refreshedPrimaryId, current primary=$currentPrimaryId.",
                            RouteRefreshLog.LOG_CATEGORY,
                        )
                    } else {
                        listener.onRoutesRefreshed(result.value)
                    }
                }
            }
        }
    }

    fun cancel() {
        scope.coroutineContext.job.cancelChildren()
    }
}
