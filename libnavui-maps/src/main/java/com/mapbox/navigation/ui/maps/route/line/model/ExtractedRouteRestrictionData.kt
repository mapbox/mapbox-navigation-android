package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Represents restricted route data extracted from a [DirectionsRoute]
 *
 * @param offset the percentage of the distance traveled along the route from the origin.
 * @param isInRestrictedSection if true this section of the route is designated as restricted.
 * @param legIndex indicates the index of the route legs array this data came from.
 */
internal data class ExtractedRouteRestrictionData(
    val offset: Double,
    val isInRestrictedSection: Boolean = false,
    val legIndex: Int = 0
)
