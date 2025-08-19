package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView

internal class RouteLineViewBasedLayerIdProvider(
    private val routeLineView: MapboxRouteLineView,
) : RouteLayerIdProvider {
    override fun getLayerId(routeId: String): String? {
        return routeLineView.getRouteMainLayerIdForFeature(routeId)
    }
}
