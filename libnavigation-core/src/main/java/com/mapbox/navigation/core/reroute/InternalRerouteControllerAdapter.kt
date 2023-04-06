package com.mapbox.navigation.core.reroute

internal class InternalRerouteControllerAdapter(
    private val originalController: NavigationRerouteController
) : InternalRerouteController, NavigationRerouteController by originalController {

    override fun reroute(callback: InternalRerouteController.RoutesCallback) {
        reroute { routes, routerOrigin ->
            callback.onNewRoutes(RerouteResult(routes, 0, routerOrigin))
        }
    }
}
