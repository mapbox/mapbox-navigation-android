package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshControllerImpl(
    private val plannedRouteRefreshController: PlannedRouteRefreshController,
    private val immediateRouteRefreshController: ImmediateRouteRefreshController,
    private val stateHolder: RouteRefreshStateHolder,
    private val refreshObserversManager: RefreshObserversManager,
    private val routeRefresherResultProcessor: RouteRefresherResultProcessor,
) : RouteRefreshController {

    override fun registerRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.registerRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

    override fun unregisterRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    ) {
        stateHolder.unregisterRouteRefreshStateObserver(routeRefreshStatesObserver)
    }

    fun registerRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.registerObserver(observer)
    }

    fun unregisterRouteRefreshObserver(observer: RouteRefreshObserver) {
        refreshObserversManager.unregisterObserver(observer)
    }

    fun destroy() {
        refreshObserversManager.unregisterAllObservers()
        stateHolder.unregisterAllRouteRefreshStateObservers()
    }

    fun requestPlannedRouteRefresh(routes: List<NavigationRoute>) {
        routeRefresherResultProcessor.reset()
        plannedRouteRefreshController.startRoutesRefreshing(routes)
    }

    override fun requestImmediateRouteRefresh() {
        val routes = plannedRouteRefreshController.routesToRefresh
        if (routes.isNullOrEmpty()) {
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
