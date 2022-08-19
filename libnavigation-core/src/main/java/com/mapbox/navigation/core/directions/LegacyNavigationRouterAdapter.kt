package com.mapbox.navigation.core.directions

import com.mapbox.navigation.base.internal.CurrentIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.internal.NavigationRouterV2

internal class LegacyNavigationRouterAdapter(
    private val legacyRouter: NavigationRouter
) : NavigationRouterV2, NavigationRouter by legacyRouter {

    override fun getRouteRefresh(
        route: NavigationRoute,
        indicesSnapshot: CurrentIndices,
        callback: NavigationRouterRefreshCallback
    ): Long {
        return legacyRouter.getRouteRefresh(route, indicesSnapshot.legIndex, callback)
    }
}
