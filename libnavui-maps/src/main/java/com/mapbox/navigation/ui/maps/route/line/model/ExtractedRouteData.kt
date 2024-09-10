package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg

/**
 * Represents data extracted from a [DirectionsRoute]
 *
 * @param offset the percentage of the distance traveled along the route from the origin.
 * @param trafficCongestionIdentifier indicates the traffic congestion for this section of the route.
 * @param roadClass the road class for this section of the route.
 * @param legIndex indicates the index of the route legs array this data came from.
 * @param isLegOrigin indicates if this item is the origin of a [RouteLeg]
 */
internal data class ExtractedRouteData(
    val offset: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String? = null,
    val legIndex: Int = 0,
    val isLegOrigin: Boolean = false,
)
