package com.mapbox.navigation.core.directions

import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback

internal class LegacyNavigationRouterAdapter(
    private val legacyRouter: NavigationRouter
) : NavigationRouterV2, NavigationRouter by legacyRouter {

    override fun getRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback
    ): Long {
        return legacyRouter.getRouteRefresh(route, routeRefreshRequestData.legIndex, callback)
    }
}
