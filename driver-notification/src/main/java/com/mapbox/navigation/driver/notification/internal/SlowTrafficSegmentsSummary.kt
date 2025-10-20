package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo

/**
 * A summary of multiple continuous slow traffic segments of a route, defined by
 * slow traffic ranges you can find in [SlowTrafficTraits.congestionRange].
 *
 * @param legIndex leg of the route with the slow traffic segment
 * @param geometryRange the range of geometry withing the leg affected by this slow traffic segment
 * @param distanceToSegmentMeters the distance to this slow traffic segment from the current user
 * position on the route
 * @param traits specific traits, that characterize subsegments of this slow traffic segment with
 * different severity (moderate traffic, heavy, ...). **Please note** that this property
 * **does not** specify *how many* of the subsegments there are, it rather is a summary of
 * different conditions within 1 single traffic congestion. This is so because different clients
 * may choose to *know* about all types of traffic, but *ignore* not-severe congestions in their UI.
 * This is also expressed in the fact that [traits] is a [Set] and not a [List]: it's not ordered by
 * its nature.
 *
 * For example, for a several slow traffic segments composed of moderate (=) and heavy (#) traffic:
 * ```
 * -----|=======##======######=====####====|--->
 *      |<--   7 SlowTrafficSegments    -->|
 * ```
 * The [traits] property would be a Set containing 2 entries: {MODERATE, HEAVY},
 * summarizing 4 moderate and 3 heavy subsegments, without describing their number or order.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class SlowTrafficSegmentsSummary(
    val legIndex: Int,
    val geometryRange: IntRange,
    val distanceToSegmentMeters: Double,
    val traits: Set<SlowTrafficTraits>,
)
