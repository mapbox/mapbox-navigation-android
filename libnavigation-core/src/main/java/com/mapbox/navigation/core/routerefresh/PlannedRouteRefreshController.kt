package com.mapbox.navigation.core.routerefresh

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.core.utils.Delayer
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class PlannedRouteRefreshController @VisibleForTesting constructor(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val delayer: Delayer,
    private val stateHolder: RouteRefreshStateHolder,
    private val listener: RouteRefresherListener,
    private val attemptListener: RoutesRefreshAttemptListener,
    private val parentScope: CoroutineScope,
    private val retryStrategy: RetryRouteRefreshStrategy,
) {

    constructor(
        routeRefresherExecutor: RouteRefresherExecutor,
        routeRefreshOptions: RouteRefreshOptions,
        stateHolder: RouteRefreshStateHolder,
        parentScope: CoroutineScope,
        listener: RouteRefresherListener,
        attemptListener: RoutesRefreshAttemptListener,
        timeProvider: Time,
    ) : this(
        routeRefresherExecutor,
        Delayer(routeRefreshOptions.intervalMillis, timeProvider),
        stateHolder,
        listener,
        attemptListener,
        parentScope,
        RetryRouteRefreshStrategy(maxAttemptsCount = MAX_RETRY_COUNT),
    )

    // null if refreshes are paused
    private var plannedRefreshScope: CoroutineScope? = CoroutineUtils.createChildScope(parentScope)
    internal var routesToRefresh: List<NavigationRoute>? = null

    fun startRoutesRefreshing(routes: List<NavigationRoute>) {
        if (plannedRefreshScope != null) {
            plannedRefreshScope?.cancel()
            plannedRefreshScope = CoroutineUtils.createChildScope(parentScope)
        }
        if (routes.isEmpty()) {
            routesToRefresh = null
            logI("Routes are empty, nothing to refresh", RouteRefreshLog.LOG_CATEGORY)
            stateHolder.reset()
            return
        }
        val routesValidationResults = routes.map { RouteRefreshValidator.validateRoute(it) }
        if (
            routesValidationResults.any { it is RouteRefreshValidator.RouteValidationResult.Valid }
        ) {
            val hasSameRoutes = routesToRefresh?.any { route1 ->
                routes.any { route2 -> route1.id == route2.id }
            } == true
            routesToRefresh = routes
            scheduleNewUpdate(routes, hasSameRoutes)
        } else {
            routesToRefresh = null
            val message =
                RouteRefreshValidator.joinValidationErrorMessages(
                    routesValidationResults.mapIndexed { index, routeValidationResult ->
                        routeValidationResult to routes[index]
                    },
                )
            val logMessage = "No routes which could be refreshed. $message"
            logI(logMessage, RouteRefreshLog.LOG_CATEGORY)
            stateHolder.onStarted()
            stateHolder.onFailure(logMessage)
            stateHolder.reset()
        }
    }

    fun pause() {
        logI("Pausing refreshes", RouteRefreshLog.LOG_CATEGORY)
        plannedRefreshScope?.cancel()
        plannedRefreshScope = null
    }

    fun resume(shouldResumeDelay: Boolean = false) {
        logI("Resuming refreshes", RouteRefreshLog.LOG_CATEGORY)
        if (plannedRefreshScope == null) {
            plannedRefreshScope = CoroutineUtils.createChildScope(parentScope)
            routesToRefresh?.let {
                if (retryStrategy.shouldRetry()) {
                    scheduleUpdateRetry(
                        it,
                        shouldNotifyOnStart = true,
                        shouldResume = shouldResumeDelay,
                    )
                }
            }
        }
    }

    private fun scheduleNewUpdate(routes: List<NavigationRoute>, shouldResume: Boolean) {
        retryStrategy.reset()
        postAttempt(shouldResume) {
            executePlannedRefresh(routes, shouldNotifyOnStart = true)
        }
    }

    private fun scheduleUpdateRetry(
        routes: List<NavigationRoute>,
        shouldNotifyOnStart: Boolean,
        shouldResume: Boolean,
    ) {
        postAttempt(shouldResume) {
            retryStrategy.onNextAttempt()
            executePlannedRefresh(routes, shouldNotifyOnStart = shouldNotifyOnStart)
        }
    }

    private fun postAttempt(shouldResume: Boolean, attemptBlock: suspend () -> Unit) {
        plannedRefreshScope?.launch {
            try {
                if (shouldResume) {
                    delayer.resumeDelay()
                } else {
                    delayer.delay()
                }
                attemptBlock()
            } catch (ex: CancellationException) {
                stateHolder.onCancel()
                throw ex
            }
        }
    }

    private suspend fun executePlannedRefresh(
        routes: List<NavigationRoute>,
        shouldNotifyOnStart: Boolean,
    ) {
        val routeRefresherResult = routeRefresherExecutor.executeRoutesRefresh(
            routes,
            startCallback = {
                if (shouldNotifyOnStart) {
                    stateHolder.onStarted()
                }
            },
        )
        when (routeRefresherResult) {
            is RoutesRefresherExecutorResult.ReplacedByNewer -> {
                logW(
                    "Planned route refresh error: " +
                        "request is skipped as a newer one is available",
                    RouteRefreshLog.LOG_CATEGORY,
                )
            }
            is RoutesRefresherExecutorResult.Finished -> {
                attemptListener.onRoutesRefreshAttemptFinished(routeRefresherResult.value)
                if (routeRefresherResult.value.anySuccess()) {
                    stateHolder.onSuccess()
                    listener.onRoutesRefreshed(routeRefresherResult.value)
                    val refreshedRoutes = listOf(
                        routeRefresherResult.value.primaryRouteRefresherResult.route,
                    ) + routeRefresherResult.value.alternativesRouteRefresherResults.map {
                        it.route
                    }
                    routesToRefresh = refreshedRoutes
                    scheduleNewUpdate(refreshedRoutes, false)
                } else {
                    if (
                        routeRefresherResult.value.anyRequestFailed() &&
                        retryStrategy.shouldRetry()
                    ) {
                        scheduleUpdateRetry(
                            routes,
                            shouldNotifyOnStart = false,
                            shouldResume = false,
                        )
                    } else {
                        stateHolder.onFailure(null)
                        listener.onRoutesRefreshed(routeRefresherResult.value)
                        scheduleNewUpdate(routes, false)
                    }
                }
            }
        }
    }

    companion object {

        const val MAX_RETRY_COUNT = 2
    }
}
