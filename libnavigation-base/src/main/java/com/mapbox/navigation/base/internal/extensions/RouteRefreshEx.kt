package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Indicates whether the route options supports route refresh.
 *
 * @receiver RouteOptions
 * @return Boolean
 */
fun RouteOptions?.supportsRefresh(): Boolean {
    if (this == null) {
        return false
    }
    val isTrafficProfile = profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    val isOverviewFull = overview() == DirectionsCriteria.OVERVIEW_FULL
    val hasCongestionOrMaxSpeed = annotationsList()?.any {
        it == DirectionsCriteria.ANNOTATION_CONGESTION ||
            it == DirectionsCriteria.ANNOTATION_MAXSPEED
    } ?: false
    return isTrafficProfile && isOverviewFull && hasCongestionOrMaxSpeed
}
