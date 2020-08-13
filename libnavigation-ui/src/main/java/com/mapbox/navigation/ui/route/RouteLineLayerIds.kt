package com.mapbox.navigation.ui.route

/**
 * Contains the values for the primary and alternative route line layer ID's
 *
 * @param primaryRouteTrafficLineLayerId the layer ID for the traffic line layer
 * @param primaryRouteLineLayerId the layer ID for the primary route line
 * @param alternativeRouteLineLayerIds the layer IDs for the alternative route line(s)
 */
data class RouteLineLayerIds(
    val primaryRouteTrafficLineLayerId: String,
    val primaryRouteLineLayerId: String,
    val alternativeRouteLineLayerIds: List<String>
)
