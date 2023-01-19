package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ImmediateRouteRefreshController(
    private val routeRefresherExecutor: RouteRefresherExecutor,
    private val stateHolder: RouteRefreshStateHolder,
    private val listener: RouteRefresherListener
) {

    private val progressCallback = object : RouteRefresherProgressCallback {

        override fun onStarted() {
            stateHolder.onStarted()
        }

        override fun onResult(routeRefresherResult: RouteRefresherResult) {
            if (routeRefresherResult.success) {
                stateHolder.onSuccess()
            } else {
                stateHolder.onFailure(null)
            }
            listener.onRoutesRefreshed(routeRefresherResult)
        }
    }

    fun requestRoutesRefresh(routes: List<NavigationRoute>, callback: (RouteRefresherResult) -> Unit) {
        if (routes.isEmpty()) {
            return
        }
        routeRefresherExecutor.postRoutesToRefresh(routes, wrapCallback(callback))
    }

    private fun wrapCallback(
        callback: (RouteRefresherResult) -> Unit
    ) = object : RouteRefresherProgressCallback {

        override fun onStarted() {
            progressCallback.onStarted()
        }

        override fun onResult(routeRefresherResult: RouteRefresherResult) {
            progressCallback.onResult(routeRefresherResult)
            callback(routeRefresherResult)
        }
    }
}
