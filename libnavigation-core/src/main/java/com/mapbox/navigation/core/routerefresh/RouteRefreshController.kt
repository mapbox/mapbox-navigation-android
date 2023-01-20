package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshController(
    private val plannedRouteRefreshController: PlannedRouteRefreshController,
    private val immediateRouteRefreshController: ImmediateRouteRefreshController,
    private val stateHolder: RouteRefreshStateHolder,
    private val refreshObserversManager: RefreshObserversManager,
    private val routeRefresherResultProcessor: RouteRefresherResultProcessor,
) {

    fun registerRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.registerObserver(observer)
    }

    fun unregisterRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.unregisterObserver(observer)
    }

    fun registerRouteRefreshStateObserver(observer: RouteRefreshStatesObserver) {
        stateHolder.registerRouteRefreshStateObserver(observer)
    }

    fun unregisterRouteRefreshStateObserver(
        observer: RouteRefreshStatesObserver
    ) {
        stateHolder.unregisterRouteRefreshStateObserver(observer)
    }

    fun destroy() {
        refreshObserversManager.unregisterAllObservers()
        stateHolder.unregisterAllRouteRefreshStateObservers()
    }

    fun requestPlannedRouteRefresh(routes: List<NavigationRoute>) {
        routeRefresherResultProcessor.reset()
        plannedRouteRefreshController.startRoutesRefreshing(routes)
    }

    fun requestImmediateRouteRefresh(routes: List<NavigationRoute>) {
        if (routes.isEmpty()) {
            return
        }
        plannedRouteRefreshController.pause()
        immediateRouteRefreshController.requestRoutesRefresh(routes) {
            if (!it.success) {
                plannedRouteRefreshController.resume()
            }
        }
    }
}
