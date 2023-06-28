package com.mapbox.navigation.core.routerefresh

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class PlannedRouteRefreshController @VisibleForTesting constructor(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val routeRefreshOptions: RouteRefreshOptions,
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
    ) : this(
        routeRefresherExecutor,
        routeRefreshOptions,
        stateHolder,
        listener,
        attemptListener,
        parentScope,
        RetryRouteRefreshStrategy(maxAttemptsCount = MAX_RETRY_COUNT)
    )

    private var plannedRefreshScope = CoroutineUtils.createChildScope(parentScope)
    private var paused = false
    var routesToRefresh: List<NavigationRoute>? = null
        private set

    fun startRoutesRefreshing(routes: List<NavigationRoute>) {
        recreateScope()
        routesToRefresh = null
        if (routes.isEmpty()) {
            logI("Routes are empty, nothing to refresh", RouteRefreshLog.LOG_CATEGORY)
            stateHolder.reset()
            return
        }
        val routesValidationResults = routes.map { RouteRefreshValidator.validateRoute(it) }
        if (
            routesValidationResults.any { it is RouteRefreshValidator.RouteValidationResult.Valid }
        ) {
            routesToRefresh = routes
            scheduleNewUpdate(routes)
        } else {
            val message =
                RouteRefreshValidator.joinValidationErrorMessages(
                    routesValidationResults.mapIndexed { index, routeValidationResult ->
                        routeValidationResult to routes[index]
                    }
                )
            val logMessage = "No routes which could be refreshed. $message"
            logI(logMessage, RouteRefreshLog.LOG_CATEGORY)
            stateHolder.onStarted()
            stateHolder.onFailure(logMessage)
            stateHolder.reset()
        }
    }

    fun pause() {
        if (!paused) {
            paused = true
            recreateScope()
        }
    }

    fun resume() {
        if (paused) {
            paused = false
            routesToRefresh?.let {
                if (retryStrategy.shouldRetry()) {
                    scheduleUpdateRetry(it, shouldNotifyOnStart = true)
                }
            }
        }
    }

    private fun scheduleNewUpdate(routes: List<NavigationRoute>) {
        retryStrategy.reset()
        postAttempt {
            executePlannedRefresh(routes, shouldNotifyOnStart = true)
        }
    }

    private fun scheduleUpdateRetry(routes: List<NavigationRoute>, shouldNotifyOnStart: Boolean) {
        postAttempt {
            retryStrategy.onNextAttempt()
            executePlannedRefresh(routes, shouldNotifyOnStart = shouldNotifyOnStart)
        }
    }

    private fun postAttempt(attemptBlock: suspend () -> Unit) {
        plannedRefreshScope.launch {
            try {
                delay(routeRefreshOptions.intervalMillis)
                attemptBlock()
            } catch (ex: CancellationException) {
                stateHolder.onCancel()
                throw ex
            }
        }
    }

    private suspend fun executePlannedRefresh(
        routes: List<NavigationRoute>,
        shouldNotifyOnStart: Boolean
    ) {
        val routeRefresherResult = routeRefresherExecutor.executeRoutesRefresh(
            routes,
            startCallback = {
                if (shouldNotifyOnStart) {
                    stateHolder.onStarted()
                }
            }
        )
        routeRefresherResult.fold(
            { logW("Planned route refresh error: $it", RouteRefreshLog.LOG_CATEGORY) },
            {
                attemptListener.onRoutesRefreshAttemptFinished(it)
                if (it.anySuccess()) {
                    stateHolder.onSuccess()
                    listener.onRoutesRefreshed(it)
                } else {
                    if (it.anyRequestFailed() && retryStrategy.shouldRetry()) {
                        scheduleUpdateRetry(routes, shouldNotifyOnStart = false)
                    } else {
                        stateHolder.onFailure(null)
                        listener.onRoutesRefreshed(it)
                        scheduleNewUpdate(routes)
                    }
                }
            }
        )
    }

    private fun recreateScope() {
        plannedRefreshScope.cancel()
        plannedRefreshScope = CoroutineUtils.createChildScope(parentScope)
    }

    companion object {

        const val MAX_RETRY_COUNT = 2
    }
}
