package com.mapbox.navigation.ui.route

/**
 * A call back that can be used to indicate that the route line layers have been added
 * to the current style.
 */
interface MapRouteLineInitializedCallback {
    /**
     * This will be called to indicate that the route line layers have been added to the current style.
     *
     * @param routeLineLayerIds the layer IDs for the primary and alternative route lines
     */
    fun onInitialized(routeLineLayerIds: RouteLineLayerIds)
}
