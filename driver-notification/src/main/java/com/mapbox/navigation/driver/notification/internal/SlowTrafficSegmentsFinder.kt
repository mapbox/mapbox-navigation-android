package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.geometryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A utility that scans a route to identify and analyze continuous segments of slow traffic.
 *
 * Note: The core logic is computationally intensive as it iterates through potentially thousands
 * of geometry points
 *
 * @param extractPoints for testing purposes only ([geometryPoints] is a static function)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class SlowTrafficSegmentsFinder(
    private val extractPoints: (RouteLeg) -> List<Point> = {
        it.geometryPoints(Constants.PRECISION_6)
    },
) {

    /**
     * Scans the route, starting from the user's current progress, to find continuous
     * stretches of traffic congestion.
     *
     * This function begins its search from the current leg and geometry index specified in
     * the [routeProgress]. It iterates forward from that point, evaluating each geometry
     * segment's congestion level.
     *
     * A "slow traffic segment" is considered a continuous sequence of geometry points where the
     * congestion value is within one of the provided [targetCongestionsRanges]. When a point
     * with a congestion level outside these ranges is encountered, the segment is considered
     * complete and is added to the results.
     * The segment is also considered complete if the next point belongs to another range within
     * [targetCongestionsRanges].
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
            val firstGeometryIndex = if (currentLegIndex == firstLegIndex) {
                currentLegProgress?.geometryIndex ?: 0
            } else {
                0
            }

            val geometry = Geometry.of(leg, extractPoints(leg)) ?: Geometry.Companion.EMPTY
            var currentSegment: SlowTrafficSegment? = null

            for (geometryIndex in firstGeometryIndex until geometry.size) {
                if (segmentsLimit <= result.size) {
                    return@withContext result
                }
                val congestion = geometry.congestion(geometryIndex)
                val congestionRange = targetCongestionsRanges.rangeOf(congestion)

                // If congestion range of [currentSegment] is not same as the one of the current
                // geometry, then [currentSegment] has just ended.
                if (currentSegment != null && currentSegment.congestionRange != congestionRange) {
                    result.add(currentSegment)
                    currentSegment = null
                }

                if (congestionRange != null) {
                    if (currentSegment == null) {
                        currentSegment = SlowTrafficSegment(
                            congestionRange = congestionRange,
                            legIndex = currentLegIndex,
                            geometryRange = geometryIndex..geometryIndex,
                            distanceToSegmentMeters = accumulatedDistance,
                            distanceMeters = 0.0,
                            freeFlowDuration = Duration.ZERO,
                            duration = Duration.ZERO,
                        )
                    }
                    currentSegment = currentSegment.updateBy(geometry, geometryIndex)
                }
                accumulatedDistance += geometry.distance(geometryIndex)
            }
            // Last congestion, if any, at the end of the leg
            currentSegment?.let { result.add(it) }
        }
        result
    }

    private fun SlowTrafficSegment.updateBy(
        geometry: Geometry,
        geometryIndex: Int,
    ): SlowTrafficSegment {
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
            geometryRange = geometryRange.first..geometryIndex,
        ).apply {
            // Each geometry consists of 2 points
            if (_points.isEmpty()) {
                _points.add(geometry.point(geometryIndex))
            }
            _points.add(geometry.point(geometryIndex + 1))
        }
    }

    private fun List<IntRange>.rangeOf(congestion: Int?) = firstOrNull { it.contains(congestion) }

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
    private val points: List<Point>,
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

    /**
     * Note: there will be `size + 1` number of points, because each geometry consists of 2 points.
     */
    fun point(index: Int): Point = points[index]

    companion object {

        fun of(leg: RouteLeg, points: List<Point>): Geometry? {
            val legDistances = leg.annotation()?.distance() ?: return null
            val legDurations = leg.annotation()?.duration() ?: return null
            val legFreeFlowSpeeds = leg.annotation()?.freeflowSpeed() ?: return null
            val legCongestions = leg.annotation()?.congestionNumeric() ?: return null

            return Geometry(
                legDistances,
                legDurations,
                legFreeFlowSpeeds,
                legCongestions,
                points,
            )
        }

        val EMPTY = Geometry(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}
