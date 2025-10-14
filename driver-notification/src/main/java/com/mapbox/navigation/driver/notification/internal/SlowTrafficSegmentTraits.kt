package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import kotlin.time.Duration

/**
 * Summary of traits of potentially multiple subsegments of 1 [SlowTrafficSegment].
 *
 * @param congestionRange a range of the congestion levels were incorporated into this
 * object. See [Constants.CongestionRange]
 * @param freeFlowDuration the duration it would take to traverse the affected range under
 * free-flow conditions
 * @param duration the duration it takes to traverse the affected range under current slow
 * traffic conditions
 * @param distanceMeters the distance of the affected range in meters
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class SlowTrafficSegmentTraits(
    val congestionRange: IntRange,
    val freeFlowDuration: Duration = Duration.Companion.ZERO,
    val duration: Duration = Duration.Companion.ZERO,
    val distanceMeters: Double = 0.0,
)
