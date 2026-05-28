package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
import kotlin.time.Duration

/**
 * A segment of a route with slow traffic.
 *
 * @param congestionRange a range of the congestion levels were incorporated into this
 * object. See [Constants.CongestionRange]
 * @param legIndex leg of the route with the slow traffic segment
 * @param geometryRange the range of geometry within the leg affected by this slow traffic segment
 * @param distanceFromRouteStartMeters the distance from the start of the route to the
 * beginning of this segment
 * @param lengthMeters the length of the affected geometry range in meters
 * @param freeFlowDuration the duration it would take to traverse the affected geometry range
 * under free-flow conditions. For points with missing free-flow speed, this falls back to the
 * point's current duration so the impact contribution is zero.
 * @param duration the duration it takes to traverse the affected geometry range under current
 * slow traffic conditions
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class SlowTrafficSegment(
    val congestionRange: IntRange,
    val legIndex: Int,
    val geometryRange: IntRange,
    val distanceFromRouteStartMeters: Double,
    val lengthMeters: Double,
    val freeFlowDuration: Duration,
    val duration: Duration,
    // Note: it is mutable only as an optimization, in order not to create too many Lists
    // during object's construction.
    internal val _points: MutableList<Point> = mutableListOf(),
    // Debug stats surfaced via SlowTrafficLogger so triage can distinguish "low impact" from
    // "low impact because freeflow data was missing". Not part of the public contract.
    internal val nullFreeFlowSegments: Int = 0,
    internal val minFreeFlowSpeed: Int? = null,
    internal val maxFreeFlowSpeed: Int? = null,
) {

    val points: List<Point> get() = _points
}
