package com.mapbox.navigation.core.routerefresh

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class PlannedRouteRefreshController @VisibleForTesting constructor(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val routeRefreshOptions: RouteRefreshOptions,
    private val stateHolder: RouteRefreshStateHolder,
    private val listener: RouteRefresherListener,
    private val cancellableHandler: CancellableHandler,
    private val retryStrategy: RetryRouteRefreshStrategy,
) {

    constructor(
        routeRefresherExecutor: RouteRefresherExecutor,
        routeRefreshOptions: RouteRefreshOptions,
        stateHolder: RouteRefreshStateHolder,
        scope: CoroutineScope,
        listener: RouteRefresherListener,
    ) : this(
        routeRefresherExecutor,
        routeRefreshOptions,
        stateHolder,
        listener,
        CancellableHandler(scope),
        RetryRouteRefreshStrategy(maxRetryCount = MAX_RETRY_COUNT)
    )

    private var paused = false
    private var routesToRefresh: List<NavigationRoute>? = null

    fun startRoutesRefreshing(routes: List<NavigationRoute>) {
        cancellableHandler.cancelAll()
        routesToRefresh = null
        if (routes.isEmpty()) {
            logI("Routes are empty", RouteRefreshLog.LOG_CATEGORY)
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
            stateHolder.onFailure(logMessage)
            stateHolder.reset()
        }
    }

    fun pause() {
        if (!paused) {
            paused = true
            cancellableHandler.cancelAll()
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
        postAttempt { executePlannedRefresh(routes, shouldNotifyOnStart = shouldNotifyOnStart) }
    }

    private fun postAttempt(attemptBlock: () -> Unit) {
        cancellableHandler.postDelayed(
            timeout = routeRefreshOptions.intervalMillis,
            block = attemptBlock,
            cancellationCallback = { stateHolder.onCancel() }
        )
    }

    private fun executePlannedRefresh(
        routes: List<NavigationRoute>,
        shouldNotifyOnStart: Boolean
    ) {
        routeRefresherExecutor.postRoutesToRefresh(
            routes,
            createCallback(routes, shouldNotifyOnStart)
        )
    }

    private fun createCallback(
        routes: List<NavigationRoute>,
        shouldNotifyOnStart: Boolean
    ): RouteRefresherProgressCallback {
        return object : RouteRefresherProgressCallback {

            override fun onStarted() {
                if (shouldNotifyOnStart) {
                    stateHolder.onStarted()
                }
            }

            override fun onResult(routeRefresherResult: RouteRefresherResult) {
                retryStrategy.onNextAttempt()
                if (routeRefresherResult.success) {
                    stateHolder.onSuccess()
                    listener.onRoutesRefreshed(routeRefresherResult)
                } else {
                    if (retryStrategy.shouldRetry()) {
                        scheduleUpdateRetry(routes, shouldNotifyOnStart = false)
                    } else {
                        stateHolder.onFailure(null)
                        listener.onRoutesRefreshed(routeRefresherResult)
                        scheduleNewUpdate(routes)
                    }
                }
            }
        }
    }

    companion object {

        const val MAX_RETRY_COUNT = 2
    }
}
