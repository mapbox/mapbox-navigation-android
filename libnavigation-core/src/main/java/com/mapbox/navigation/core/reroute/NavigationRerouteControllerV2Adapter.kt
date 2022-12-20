package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationRerouteControllerV2Adapter(
    private val oldReroute: NavigationRerouteController
) : NavigationRerouteController by oldReroute, NavigationRerouteControllerV2 {

    override fun reroute(
        params: RerouteParameters,
        callback: NavigationRerouteController.RoutesCallback
    ) {
        oldReroute.reroute(callback)
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        oldReroute.reroute(routesCallback)
    }
}
