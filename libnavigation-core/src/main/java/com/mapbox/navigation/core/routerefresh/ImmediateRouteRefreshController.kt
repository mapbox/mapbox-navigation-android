package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ImmediateRouteRefreshController(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val plannedRefreshController: Pausable,
    private val stateHolder: RouteRefreshStateHolder,
    private val listener: RouteRefresherListener
) {

    private val callback = object : RouteRefresherProgressCallback {

        override fun onStarted() {
            stateHolder.onStarted()
        }

        override fun onResult(routeRefresherResult: RouteRefresherResult) {
            if (routeRefresherResult.success) {
                stateHolder.onSuccess()
            } else {
                stateHolder.onFailure(null)
                plannedRefreshController.resume()
            }
            listener.onRoutesRefreshed(routeRefresherResult)
        }
    }

    fun requestRoutesRefresh(routes: List<NavigationRoute>) {
        if (routes.isEmpty()) {
            return
        }
        plannedRefreshController.pause()
        routeRefresherExecutor.postRoutesToRefresh(routes, callback)
    }
}
