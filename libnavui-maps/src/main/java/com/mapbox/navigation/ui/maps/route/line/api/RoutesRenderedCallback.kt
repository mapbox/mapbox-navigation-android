package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.route.NavigationRoute

/**
 Callback to be notified of rendered routes.
 */
interface RoutesRenderedCallback {

    /**
     * Map instance where the routes are expected to be rendered.
     */
    val map: MapboxMap

    /**
     * Invoked when routes are rendered.
     *
     * @param ids ids of the routes that were rendered (correspond to [NavigationRoute.id]).
     */
    fun onRoutesRendered(ids: List<String>)
}
