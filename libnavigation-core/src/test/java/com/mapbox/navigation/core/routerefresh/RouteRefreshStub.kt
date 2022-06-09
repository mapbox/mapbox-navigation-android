package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouterFactory.buildNavigationRouterRefreshError
import com.mapbox.navigation.core.directions.session.RouteRefresh

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RouteRefreshStub : RouteRefresh {

    private var requestId = 0L
    private val handlers = mutableMapOf<String, RouteRefreshHandler>()

    override fun requestRouteRefresh(
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
    ): Long {
        val currentRequestId = requestId++
        val handler = handlers[route.id]
        if (handler != null) {
            handler(route, legIndex, callback)
        } else {
            callback.onFailure(buildNavigationRouterRefreshError("handle isn't configured yet"))
        }

        return currentRequestId
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
    }

    /***
     * Tne next route refresh requests will return the actual routes
     */
    fun setRefreshedRoute(refreshedRoute: NavigationRoute) {
        handlers[refreshedRoute.id] = { _, _, callback ->
            // TODO: refresh legs only from the passed index
            callback.onRefreshReady(refreshedRoute)
        }
    }

    /***
     * The next route refresh requests for the given route route will fail
     */
    fun failRouteRefresh(navigationRouteId: String) {
        handlers[navigationRouteId] = { _, _, callback ->
            callback.onFailure(
                buildNavigationRouterRefreshError(
                    "Failed by RouteRefreshStub#failPendingRefreshRequest"
                )
            )
        }
    }

    fun doNotRespondForRouteRefresh(navigationRouteId: String) {
        handlers[navigationRouteId] = { _, _, _ -> }
    }
}

private typealias RouteRefreshHandler = (
    route: NavigationRoute,
    legIndex: Int,
    callback: NavigationRouterRefreshCallback
) -> Unit
