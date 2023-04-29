package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.utils.internal.logW

internal class InternalRerouteControllerAdapter(
    private val originalController: NavigationRerouteController
) : InternalRerouteController, NavigationRerouteController by originalController {

    override fun reroute(callback: InternalRerouteController.RoutesCallback) {
        logW(
            "Reroute is handled by custom reroute controller with legacy interface. " +
                "Using 0 for leg index as a fallback. Consider using default reroute controller.",
            LOG_CATEGORY
        )
        reroute { routes, routerOrigin ->
            callback.onNewRoutes(RerouteResult(routes, 0, routerOrigin))
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "MapboxRerouteController"
    }
}
