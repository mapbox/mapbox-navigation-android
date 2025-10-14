package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A utility that scans a route to identify and analyze continuous segments of slow traffic.
 *
 * Note: The core logic is computationally intensive as it iterates through potentially thousands
 * of geometry points
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class SlowTrafficSegmentsFinder {

    /**
     * Scans the route, starting from the user's current progress, to find continuous
     * stretches of traffic congestion.
     *
     * This function begins its search from the current leg and geometry index specified in
     * the [routeProgress]. It iterates forward from that point, evaluating each geometry
     * segment's congestion level.
     *
     * A "slow traffic segment" is considered a continuous sequence of geometry points where the
     * congestion value is within any of the provided [targetCongestionsRanges]. When a point
     * with a congestion level outside these ranges is encountered, the segment is considered
     * complete and is added to the results.
     *
     * A single returned [SlowTrafficSegment] can contain multiple
     * levels of congestion (e.g., both "moderate" and "heavy"). The `traits` property of the
     * segment provides a breakdown of the distance and duration for each specific congestion
     * range found within that segment.
     *
     * @param routeProgress The current progress along the route. The search for traffic
     * will begin from this point forward.
     * @param targetCongestionsRanges A list of integer ranges that define what constitutes
     * "slow traffic". See also [com.mapbox.navigation.base.internal.utils.Constants.CongestionRange].
     * @param legsLimit An optional optimization parameter to limit how many legs of the route
     * are scanned, starting from the current one.
     * @param segmentsLimit An optional optimization parameter to stop the search after a
     * certain number of slow segments have been found.
     * @return A list of [SlowTrafficSegment] objects representing the identified stretches of
     * congestion. The list is ordered by their appearance on the route.
     */
    suspend fun findSlowTrafficSegments(
        routeProgress: RouteProgress,
        targetCongestionsRanges: List<IntRange>,
        legsLimit: Int = Int.MAX_VALUE,
        segmentsLimit: Int = Int.MAX_VALUE,
    ): List<SlowTrafficSegment> = withContext(Dispatchers.Default) {
        val result = mutableListOf<SlowTrafficSegment>()

        val currentLegProgress = routeProgress.currentLegProgress
        val firstLegIndex = currentLegProgress?.legIndex ?: 0
        val legs = routeProgress.route.legs()
            ?.drop(firstLegIndex)
            .orEmpty()
            .take(legsLimit)

        var accumulatedDistance = 0.0
        legs.forEachIndexed { legListIndex, leg ->
            val currentLegIndex = firstLegIndex + legListIndex
            var firstGeometryIndex = if (currentLegIndex == firstLegIndex) {
                currentLegProgress?.geometryIndex ?: 0
            } else {
                0
            }

            val geometry = Geometry.Companion.of(leg) ?: Geometry.Companion.EMPTY
            var traitsMap: MutableMap<IntRange, SlowTrafficSegmentTraits>? = null
            var trafficStartIndex: Int? = null
            var trafficStartMeters: Double? = null

            for (geometryIndex in firstGeometryIndex until geometry.size) {
                var congestion = geometry.congestion(geometryIndex)
                if (congestion.isIn(targetCongestionsRanges)) {
                    traitsMap = traitsMap ?: mutableMapOf()
                    trafficStartIndex = trafficStartIndex ?: geometryIndex
                    trafficStartMeters = trafficStartMeters ?: accumulatedDistance

                    val congestionRange = targetCongestionsRanges.rangeOf(congestion) ?: continue
                    val traits = traitsMap.getOrPut(congestionRange) {
                        SlowTrafficSegmentTraits(congestionRange)
                    }
                    // Let's update the existing traits with new data
                    traitsMap[congestionRange] = traits.updateBy(
                        geometry,
                        geometryIndex,
                    )
                } else {
                    if (traitsMap != null &&
                        trafficStartIndex != null &&
                        trafficStartMeters != null
                    ) {
                        // Slow traffic section has just ended
                        result.add(
                            SlowTrafficSegment(
                                legIndex = currentLegIndex,
                                geometryRange = trafficStartIndex until geometryIndex,
                                distanceToSegmentMeters = trafficStartMeters,
                                traits = traitsMap.values.toSet(),
                            ),
                        )
                    }
                    traitsMap = null
                    trafficStartIndex = null
                    trafficStartMeters = null
                    if (segmentsLimit <= result.size) {
                        return@withContext result
                    }
                }
                accumulatedDistance += geometry.distance(geometryIndex)
            }
            // Last congestion, if any
            if (traitsMap != null && trafficStartIndex != null && trafficStartMeters != null) {
                result.add(
                    SlowTrafficSegment(
                        currentLegIndex,
                        trafficStartIndex until geometry.size,
                        trafficStartMeters,
                        traitsMap.values.toSet(),
                    ),
                )
            }
        }
        result
    }

    private fun SlowTrafficSegmentTraits.updateBy(
        geometry: Geometry,
        geometryIndex: Int,
    ): SlowTrafficSegmentTraits {
        val legFreeFlowSpeed = geometry.freeFlowSpeed(geometryIndex)
        val freeFlowDurationSec = when {
            legFreeFlowSpeed != null -> {
                // Speed is km/h, but distance is in the meters.
                // Applying conversion of the speed from km/h -> m/s
                geometry.distance(geometryIndex) * KM_PER_H_TO_M_PER_SEC_RATE / legFreeFlowSpeed
            }
            0.0 < this.distanceMeters -> {
                // Adding average duration based on the previous geometry
                val avgTimePerMeter =
                    this.freeFlowDuration.inWholeSeconds.toDouble() / this.distanceMeters
                geometry.distance(geometryIndex) * avgTimePerMeter
            }
            else -> 0.0
        }
        return this.copy(
            freeFlowDuration = this.freeFlowDuration + freeFlowDurationSec.seconds,
            duration = this.duration + geometry.duration(geometryIndex),
            distanceMeters = this.distanceMeters + geometry.distance(geometryIndex),
        )
    }

    private fun List<IntRange>.rangeOf(congestion: Int?) = firstOrNull { it.contains(congestion) }
    private fun Int?.isIn(ranges: List<IntRange>) = ranges.rangeOf(this) != null

    private companion object {
        // Calc time with seconds:
        // dist_m / speed_km_h = dist_m / (speed_km_h * 1000 / 3600) = dist_m * 3.6 / speed_km_h = duration_sec
        private const val KM_PER_H_TO_M_PER_SEC_RATE = 3.6
    }
}

/**
 * A wrapper around the 4 lists of [RouteLeg] for code readability
 */
private class Geometry(
    private val distances: List<Double>,
    private val durations: List<Double>,
    private val freeFlowSpeeds: List<Int?>,
    private val congestions: List<Int?>,
) {
    val size: Int = listOf(
        distances.size,
        durations.size,
        freeFlowSpeeds.size,
        congestions.size,
    ).min()
    fun distance(index: Int): Double = distances[index]
    fun duration(index: Int): Duration = durations[index].seconds
    fun freeFlowSpeed(index: Int): Int? = freeFlowSpeeds[index]
    fun congestion(index: Int): Int? = congestions[index]

    companion object {
        fun of(leg: RouteLeg): Geometry? {
            val legDistances = leg.annotation()?.distance() ?: return null
            val legDurations = leg.annotation()?.duration() ?: return null
            val legFreeFlowSpeeds = leg.annotation()?.freeflowSpeed() ?: return null
            val legCongestions = leg.annotation()?.congestionNumeric() ?: return null

            return Geometry(
                legDistances,
                legDurations,
                legFreeFlowSpeeds,
                legCongestions,
            )
        }

        val EMPTY = Geometry(emptyList(), emptyList(), emptyList(), emptyList())
    }
}
