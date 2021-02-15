@file:JvmName("DirectionsRefreshEx")

package com.mapbox.navigation.base.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Indicates whether the route options supports route refresh.
 *
 * To qualify for the route refresh feature, the [RouteOptions] need to include:
 * - [DirectionsCriteria.PROFILE_DRIVING_TRAFFIC]
 * - [DirectionsCriteria.OVERVIEW_FULL]
 * and one of:
 * - [DirectionsCriteria.ANNOTATION_CONGESTION]
 * - [DirectionsCriteria.ANNOTATION_MAXSPEED]
 * - [DirectionsCriteria.ANNOTATION_SPEED]
 * - [DirectionsCriteria.ANNOTATION_DURATION]
 * - [DirectionsCriteria.ANNOTATION_DISTANCE]
 *
 * @receiver RouteOptions
 * @return Boolean
 */
fun RouteOptions?.supportsRouteRefresh(): Boolean {
    if (this == null) {
        return false
    }
    val isTrafficProfile = profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    val isOverviewFull = overview() == DirectionsCriteria.OVERVIEW_FULL
    val hasCongestionOrMaxSpeed = annotationsList()?.any {
        it == DirectionsCriteria.ANNOTATION_CONGESTION ||
            it == DirectionsCriteria.ANNOTATION_MAXSPEED ||
            it == DirectionsCriteria.ANNOTATION_SPEED ||
            it == DirectionsCriteria.ANNOTATION_DURATION ||
            it == DirectionsCriteria.ANNOTATION_DISTANCE
    } ?: false
    return isTrafficProfile && isOverviewFull && hasCongestionOrMaxSpeed
}
