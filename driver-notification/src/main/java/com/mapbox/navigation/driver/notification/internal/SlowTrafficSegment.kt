package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import kotlin.time.Duration

/**
 * A segment of a route with slow traffic.
 *
 * @param congestionRange a range of the congestion levels were incorporated into this
 * object. See [Constants.CongestionRange]
 * @param legIndex leg of the route with the slow traffic segment
 * @param geometryRange the range of geometry withing the leg affected by this slow traffic segment
 * @param distanceToSegmentMeters the distance to this slow traffic segment from the current user
 * position on the route
 * @param distanceMeters the distance of the affected geometry range in meters
 * @param freeFlowDuration the duration it would take to traverse the affected geometry range under
 * free-flow conditions
 * @param duration the duration it takes to traverse the affected geometry range under current slow
 * traffic conditions
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class SlowTrafficSegment(
    val congestionRange: IntRange,
    val legIndex: Int,
    val geometryRange: IntRange,
    val distanceToSegmentMeters: Double,
    val distanceMeters: Double,
    val freeFlowDuration: Duration,
    val duration: Duration,
)
