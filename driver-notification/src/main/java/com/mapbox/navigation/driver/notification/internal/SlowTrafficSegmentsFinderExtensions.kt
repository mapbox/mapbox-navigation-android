package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.driver.notification.internal.SlowTrafficLogger.logSegment
import com.mapbox.navigation.driver.notification.internal.SlowTrafficLogger.logSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calls [SlowTrafficSegmentsFinder.findSlowTrafficSegments] and creates multiple summaries of
 * its results for each **continuous** segments groups.
 *
 * For example, for next multiple slow traffic segment composed of
 * moderate (=) and heavy (#) traffic:
 *  ```
 *  -----|====##======###|----|##|----|==|----|=#=#|---->
 *  ```
 *  ... this function will return 4 instances of [SlowTrafficSegmentsSummary], because
 *  4 continuous stretches of slow traffic are present there.
 *
 * A single returned [SlowTrafficSegmentsSummary] can contain multiple
 * levels of congestion (e.g., both "moderate" and "heavy"). The `traits` property of the
 * segment provides a breakdown of the distance and duration for each specific congestion
 * range found within that segment.
 *
 * @param routeProgress same as in [SlowTrafficSegmentsFinder.findSlowTrafficSegments]
 * @param targetCongestionsRanges same as in [SlowTrafficSegmentsFinder.findSlowTrafficSegments]
 * @return A list of [SlowTrafficSegmentsSummary] objects representing continuous stretches of
 * congestions. The list is ordered by their appearance on the route.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun SlowTrafficSegmentsFinder.findAndSummarizeSlowTrafficSegments(
    routeProgress: RouteProgress,
    targetCongestionsRanges: List<IntRange>,
): List<SlowTrafficSegmentsSummary> = withContext(Dispatchers.Default) {
    val shouldLog = SlowTrafficLogger.shouldLogNow()
    val segments = findSlowTrafficSegments(
        routeProgress = routeProgress,
        targetCongestionsRanges = targetCongestionsRanges,
    )

    val summaries = mutableListOf<SlowTrafficSegmentsSummary>()
    var currentSummary: SlowTrafficSegmentsSummary? = null
    var traitsMap: MutableMap<IntRange, SlowTrafficTraits>? = null
    var currentPoints: MutableList<Point>? = null
    var eventIndex = 0
    var longestSegmentDistance = 0.0
    var dominantCongestionRange: IntRange = IntRange.EMPTY

    for ((segmentIndex, segment) in segments.withIndex()) {
        val segmentTraits = segment.toTraits()

        val isContiguous = currentSummary != null &&
            segment.legIndex == currentSummary.legIndex &&
            segment.geometryRange.first == currentSummary.geometryRange.last + 1
        if (currentSummary == null || !isContiguous) {
            if (currentSummary != null) {
                summaries.add(
                    currentSummary,
                    traitsMap.orEmpty(),
                    currentPoints.orEmpty(),
                    dominantCongestionRange,
                )
                eventIndex++
                traitsMap = null
                currentPoints = null
                longestSegmentDistance = 0.0
                dominantCongestionRange = IntRange.EMPTY
            }
            currentSummary = SlowTrafficSegmentsSummary(
                legIndex = segment.legIndex,
                geometryRange = segment.geometryRange,
                distanceToSegmentMeters = segment.distanceToSegmentMeters,
                traits = emptySet(),
                points = emptyList(),
                dominantCongestionRange = IntRange.EMPTY,
            )
        }
        if (segment.distanceMeters > longestSegmentDistance) {
            longestSegmentDistance = segment.distanceMeters
            dominantCongestionRange = segment.congestionRange
        }
        if (shouldLog) logSegment(segmentIndex, segment, isNew = currentPoints == null, eventIndex)
        traitsMap = traitsMap ?: mutableMapOf()

        // Skip the first point of subsequent segments to avoid duplicating the boundary
        // point that is shared between adjacent segments (last point of A == first point of B).
        val pointsToAdd = if (currentPoints == null) segment.points else segment.points.drop(1)
        currentPoints = (currentPoints ?: mutableListOf()).apply { addAll(pointsToAdd) }

        currentSummary = currentSummary.copy(
            geometryRange = currentSummary.geometryRange.first..segment.geometryRange.last,
        )
        val existingTraits = traitsMap.getOrPut(segment.congestionRange) {
            SlowTrafficTraits(segment.congestionRange)
        }
        traitsMap[segment.congestionRange] = existingTraits.add(segmentTraits)
    }

    currentSummary?.let {
        summaries.add(it, traitsMap.orEmpty(), currentPoints.orEmpty(), dominantCongestionRange)
    }

    if (shouldLog) summaries.forEachIndexed { index, summary -> logSummary(index, summary) }
    summaries
}

private fun SlowTrafficSegment.toTraits(): SlowTrafficTraits {
    return SlowTrafficTraits(
        congestionRange = this.congestionRange,
        freeFlowDuration = this.freeFlowDuration,
        duration = this.duration,
        distanceMeters = this.distanceMeters,
    )
}

private fun MutableList<SlowTrafficSegmentsSummary>.add(
    new: SlowTrafficSegmentsSummary,
    traits: Map<IntRange, SlowTrafficTraits>,
    points: List<Point>,
    dominantCongestionRange: IntRange,
) {
    add(
        new.copy(
            traits = traits.values.toSet(),
            points = points,
            dominantCongestionRange = dominantCongestionRange,
        ),
    )
}

private fun SlowTrafficTraits.add(
    another: SlowTrafficTraits,
): SlowTrafficTraits {
    return this.copy(
        freeFlowDuration = this.freeFlowDuration + another.freeFlowDuration,
        duration = this.duration + another.duration,
        distanceMeters = this.distanceMeters + another.distanceMeters,
    )
}
