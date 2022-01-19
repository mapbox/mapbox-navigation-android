package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.route.toNavigationRoutes

internal class LegacyRerouteControllerAdapter(
    private val legacyRerouteController: RerouteController
) : NavigationRerouteController, RerouteController by legacyRerouteController {
    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        legacyRerouteController.reroute { routes ->
            callback.onNewRoutes(routes.toNavigationRoutes())
        }
    }
}
