package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesObserver
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
) {

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
            if (it.value?.success == false) {
                plannedRouteRefreshController.resume()
            }
        }
    }

    internal fun registerRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.registerObserver(observer)
    }

    internal fun unregisterRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.unregisterObserver(observer)
    }

    internal fun destroy() {
        refreshObserversManager.unregisterAllObservers()
        stateHolder.unregisterAllRouteRefreshStateObservers()
        // first unregister observers, then cancel scope - otherwise we dispatch CANCELLED state from onDestroy
        routeRefreshParentJob.cancel()
    }

    internal fun requestPlannedRouteRefresh(routes: List<NavigationRoute>) {
        routeRefresherResultProcessor.reset()
        plannedRouteRefreshController.startRoutesRefreshing(routes)
    }
}
