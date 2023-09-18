package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.RoutesInvalidatedObserver
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.Job

/**
 * This class lets you manage route refreshes.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteRefreshController internal constructor(
    private val routeRefreshParentJob: Job,
    private val plannedRouteRefreshController: PlannedRouteRefreshController,
    private val immediateRouteRefreshController: ImmediateRouteRefreshController,
    private val stateHolder: RouteRefreshStateHolder,
    private val refreshObserversManager: RefreshObserversManager,
    private val routeRefresherResultProcessor: RouteRefresherResultProcessor,
) : RoutesObserver {

    /**
     * Register a [RouteRefreshStatesObserver] to be notified of Route refresh state changes.
     *
     * @param routeRefreshStatesObserver RouteRefreshStatesObserver
     */
    fun registerRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.registerRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

    /**
     * Unregisters a [RouteRefreshStatesObserver].
     *
     * @param routeRefreshStatesObserver RouteRefreshStatesObserver
     */
    fun unregisterRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.unregisterRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

    /**
     * Immediately refresh current navigation routes.
     * Listen for refreshed routes using [RoutesObserver].
     *
     * The on-demand refresh request is not guaranteed to succeed and call the [RoutesObserver],
     * [requestImmediateRouteRefresh] invocations cannot be coupled with
     * [RoutesObserver.onRoutesChanged] callbacks for state management.
     * You can use [registerRouteRefreshStateObserver] to monitor refresh statuses independently.
     * NOTE: the invocation will have no effect if another route refresh request is in progress.
     */
    fun requestImmediateRouteRefresh() {
        val routes = plannedRouteRefreshController.routesToRefresh
        if (routes.isNullOrEmpty()) {
            logI("No routes to refresh", RouteRefreshLog.LOG_CATEGORY)
            stateHolder.onStarted()
            stateHolder.onFailure("No routes to refresh")
            return
        }
        plannedRouteRefreshController.pause()
        immediateRouteRefreshController.requestRoutesRefresh(routes) {
            if (it.isValue) {
                plannedRouteRefreshController.resume()
            }
        }
    }

    /**
     * Pauses route refreshes. If route refreshes are already paused, this is a no-op.
     * After this invocation, no route refreshes will be done,
     * except for the ones that are requested explicitly via [refreshRoutesImmediately].
     * To resume route refreshes, invoke [resumeRouteRefreshes].
     */
    fun pauseRouteRefreshes() {
        plannedRouteRefreshController.pause()
    }

    /**
     * Resumes route refreshes that were paused via [pauseRouteRefreshes].
     * If route refreshes are not paused, this is a no-op.
     */
    fun resumeRouteRefreshes() {
        plannedRouteRefreshController.resume()
    }

    internal fun registerRoutesInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        refreshObserversManager.registerInvalidatedObserver(observer)
    }

    internal fun unregisterRoutesInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        refreshObserversManager.unregisterInvalidatedObserver(observer)
    }

    internal fun registerRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.registerRefreshObserver(observer)
    }

    internal fun unregisterRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.unregisterRefreshObserver(observer)
    }

    internal fun destroy() {
        refreshObserversManager.unregisterAllObservers()
        stateHolder.unregisterAllRouteRefreshStateObservers()
        // first unregister observers, then cancel scope - otherwise we dispatch CANCELLED state from onDestroy
        routeRefreshParentJob.cancel()
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        if (result.reason != RoutesExtra.ROUTES_UPDATE_REASON_REFRESH) {
            routeRefresherResultProcessor.reset()
            immediateRouteRefreshController.cancel()
            plannedRouteRefreshController.startRoutesRefreshing(result.navigationRoutes)
        }
    }
}
