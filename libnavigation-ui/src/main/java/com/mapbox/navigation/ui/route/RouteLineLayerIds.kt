package com.mapbox.navigation.ui.route

/**
 * Contains the values for the primary and alternative route line layer ID's
 *
 * @param primaryRouteLineLayerId the layer ID for the primary route line
 * @param alternativeRouteLineLayerId the layer ID for the alternative route line(s)
 */
data class RouteLineLayerIds(val primaryRouteLineLayerId: String, val alternativeRouteLineLayerId: String)
