package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Interface allowing to modify [RouteOptions] during a reroute request with the default
 * Navigation SDK's reroute controller.
 */
interface RerouteOptionsAdapter {

    /**
     * Allows to modify [RouteOptions] before a reroute request is sent.
     */
    fun onRouteOptions(routeOptions: RouteOptions): RouteOptions
}
