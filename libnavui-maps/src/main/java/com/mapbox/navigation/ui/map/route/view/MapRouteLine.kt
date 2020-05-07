package com.mapbox.navigation.ui.map.route.view

import com.mapbox.maps.Style
import com.mapbox.navigation.ui.base.View
import com.mapbox.navigation.ui.base.map.route.model.RouteLineState

class MapRouteLine(style: Style) : View<RouteLineState> {

    // todo port waypoint layer

    private val primaryRouteRenderer = RouteRenderer<PrimaryRouteData>(
        style,
        "mapbox_navigation_primary_route_source",
        "mapbox_navigation_primary_route_layer",
        "mapbox_navigation_primary_shield_layer"
    )

    private val alternativeRouteRenderer = RouteRenderer<AlternativeRouteData>(
        style,
        "mapbox_navigation_alternative_route_layer",
        "mapbox_navigation_alternative_route_source",
        "mapbox_navigation_alternative_shield_layer"
    )

    override fun render(state: RouteLineState) {
        primaryRouteRenderer.update(PrimaryRouteData(state))
        alternativeRouteRenderer.update(AlternativeRouteData(state))
    }
}
