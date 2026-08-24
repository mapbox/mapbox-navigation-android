package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
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
 * @param route same as in [SlowTrafficSegmentsFinder.findSlowTrafficSegments]
 * @param targetCongestionsRanges same as in [SlowTrafficSegmentsFinder.findSlowTrafficSegments]
 * @return A list of [SlowTrafficSegmentsSummary] objects representing continuous stretches of
 * congestions. The list is ordered by their appearance on the route.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun SlowTrafficSegmentsFinder.findAndSummarizeSlowTrafficSegments(
    route: DirectionsRoute,
    targetCongestionsRanges: List<IntRange>,
): List<SlowTrafficSegmentsSummary> = withContext(Dispatchers.Default) {
    val routeUuid = route.requestUuid()
    SlowTrafficLogger.logRouteChanged(routeUuid)
    val shouldLog = SlowTrafficLogger.shouldLogNow()
    val segments = findSlowTrafficSegments(
        route = route,
        targetCongestionsRanges = targetCongestionsRanges,
    )

    val summaries = mutableListOf<SlowTrafficSegmentsSummary>()
    val perSummaryStats = mutableListOf<EventStats>()
    var currentSummary: SlowTrafficSegmentsSummary? = null
    var traitsMap: MutableMap<IntRange, SlowTrafficTraits>? = null
    var currentPoints: MutableList<Point>? = null
    var eventIndex = 0
    var longestSegmentDistance = 0.0
    var dominantCongestionRange: IntRange = IntRange.EMPTY
    var summarySkipped = 0
    var summaryCovered = 0
    var summaryMinFreeFlow: Int? = null
    var summaryMaxFreeFlow: Int? = null

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
                if (shouldLog) {
                    perSummaryStats.add(
                        EventStats(
                            skipped = summarySkipped,
                            covered = summaryCovered,
                            minFreeFlow = summaryMinFreeFlow,
                            maxFreeFlow = summaryMaxFreeFlow,
                        ),
                    )
                    summarySkipped = 0
                    summaryCovered = 0
                    summaryMinFreeFlow = null
                    summaryMaxFreeFlow = null
                }
                eventIndex++
                traitsMap = null
                currentPoints = null
                longestSegmentDistance = 0.0
                dominantCongestionRange = IntRange.EMPTY
            }
            currentSummary = SlowTrafficSegmentsSummary(
                legIndex = segment.legIndex,
                geometryRange = segment.geometryRange,
                distanceFromRouteStartMeters = segment.distanceFromRouteStartMeters,
                traits = emptySet(),
                points = emptyList(),
                dominantCongestionRange = IntRange.EMPTY,
            )
        }
        if (segment.lengthMeters > longestSegmentDistance) {
            longestSegmentDistance = segment.lengthMeters
            dominantCongestionRange = segment.congestionRange
        }
        // Stat aggregation only matters when this interval's logging will fire; skipped otherwise
        // to keep the per-segment hot path cheap when logging is disabled.
        if (shouldLog) {
            summarySkipped += segment.nullFreeFlowSegments
            summaryCovered += (segment.geometryRange.last - segment.geometryRange.first + 1) -
                segment.nullFreeFlowSegments
            summaryMinFreeFlow =
                listOfNotNull(summaryMinFreeFlow, segment.minFreeFlowSpeed).minOrNull()
            summaryMaxFreeFlow =
                listOfNotNull(summaryMaxFreeFlow, segment.maxFreeFlowSpeed).maxOrNull()
            logSegment(segmentIndex, segment, isNew = currentPoints == null, eventIndex)
        }
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
        if (shouldLog) {
            perSummaryStats.add(
                EventStats(
                    skipped = summarySkipped,
                    covered = summaryCovered,
                    minFreeFlow = summaryMinFreeFlow,
                    maxFreeFlow = summaryMaxFreeFlow,
                ),
            )
        }
    }

    if (shouldLog) {
        if (summaries.isEmpty()) {
            SlowTrafficLogger.logNoEvents(routeUuid, targetCongestionsRanges)
        } else {
            summaries.forEachIndexed { index, summary ->
                val stats = perSummaryStats[index]
                logSummary(
                    index = index,
                    summary = summary,
                    nullFreeFlowSegments = stats.skipped,
                    coveredSegments = stats.covered,
                    minFreeFlowSpeed = stats.minFreeFlow,
                    maxFreeFlowSpeed = stats.maxFreeFlow,
                )
            }
        }
    }
    summaries
}

private data class EventStats(
    val skipped: Int,
    val covered: Int,
    val minFreeFlow: Int?,
    val maxFreeFlow: Int?,
)

private fun SlowTrafficSegment.toTraits(): SlowTrafficTraits {
    return SlowTrafficTraits(
        congestionRange = this.congestionRange,
        freeFlowDuration = this.freeFlowDuration,
        duration = this.duration,
        lengthMeters = this.lengthMeters,
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
        lengthMeters = this.lengthMeters + another.lengthMeters,
    )
}
