package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshController internal constructor(
    private val routeRefreshScope: CoroutineScope,
    private val plannedRouteRefreshController: PlannedRouteRefreshController,
    private val immediateRouteRefreshController: ImmediateRouteRefreshController,
    private val stateHolder: RouteRefreshStateHolder,
    private val refreshObserversManager: RefreshObserversManager,
    private val routeRefresherResultProcessor: RouteRefresherResultProcessor,
) {

    fun registerRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.registerRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

    fun unregisterRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.unregisterRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

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
        routeRefreshScope.cancel()
    }

    internal fun requestPlannedRouteRefresh(routes: List<NavigationRoute>) {
        routeRefresherResultProcessor.reset()
        plannedRouteRefreshController.startRoutesRefreshing(routes)
    }
}
