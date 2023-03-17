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

    /**
     * Routes rendering has been cancelled, because newer routes are queued to be rendered.
     *
     * @param ids ids of the routes whose rendering has been cancelled (correspond to [NavigationRoute.id]).
     */
    fun onRoutesRenderingCancelled(ids: List<String>)
}
