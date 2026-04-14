package com.mapbox.navigation.driver.notification.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.geometryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.IdentityHashMap
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

    private var cachedRoute: DirectionsRoute? = null
    private val pointsCache = IdentityHashMap<RouteLeg, List<Point>>()

    /**
     * Returns cached geometry points for [leg], decoding them via [extractPoints] only on the
     * first call per unique [route] instance. The cache is cleared when [route] changes.
     */
    @Synchronized
    private fun getPoints(route: DirectionsRoute, leg: RouteLeg): List<Point> {
        if (cachedRoute !== route) {
            cachedRoute = route
            pointsCache.clear()
        }
        return pointsCache.getOrPut(leg) { extractPoints(leg) }
    }

    /**
     * Scans the route to find continuous stretches of traffic congestion.
     *
     * A "slow traffic segment" is a continuous sequence of geometry points where the congestion
     * value falls within one of the provided [targetCongestionsRanges]. A segment ends when a
     * point outside all ranges is encountered, or when the congestion switches to a different
     * range within [targetCongestionsRanges].
     *
     * Callers are responsible for filtering segments that are behind the user's current position
     * using [SlowTrafficSegment.distanceFromRouteStartMeters].
     *
     * @param route The route to scan.
     * @param targetCongestionsRanges A list of integer ranges that define what constitutes
     * "slow traffic". See also [com.mapbox.navigation.base.internal.utils.Constants.CongestionRange].
     * @param currentLeg Index of the leg to start scanning from. Legs before this index are
     * skipped (geometry is not decoded for them). Their distance is taken from [RouteLeg.distance]
     * so that [SlowTrafficSegment.distanceFromRouteStartMeters] remains absolute.
     * @param firstGeometryIndex Index of the geometry point within [currentLeg] to start scanning
     * from. Geometry points before this index are skipped, but their distances are still accumulated
     * so that [SlowTrafficSegment.distanceFromRouteStartMeters] remains absolute.
     * @param legsLimit Maximum number of legs to scan, starting from [currentLeg].
     * @param segmentsLimit Maximum number of segments to return. Scanning stops as soon as this
     * count is reached.
     * @return A list of [SlowTrafficSegment] objects ordered by their appearance on the route.
     */
    suspend fun findSlowTrafficSegments(
        route: DirectionsRoute,
        targetCongestionsRanges: List<IntRange>,
        currentLeg: Int = 0,
        firstGeometryIndex: Int = 0,
        legsLimit: Int = Int.MAX_VALUE,
        segmentsLimit: Int = Int.MAX_VALUE,
    ): List<SlowTrafficSegment> = withContext(Dispatchers.Default) {
        val result = mutableListOf<SlowTrafficSegment>()
        val legs = route.legs().orEmpty()

        // Accumulate distance for skipped legs using the leg-level distance field,
        // avoiding geometry decoding for legs the user has already passed.
        var accumulatedDistance = legs.take(currentLeg).sumOf { it.distance() ?: 0.0 }

        val legRange = currentLeg until minOf(currentLeg + legsLimit, legs.size)
        for (legIndex in legRange) {
            val leg = legs[legIndex]
            val geometry = Geometry.of(leg, getPoints(route, leg)) ?: Geometry.EMPTY
            var currentSegment: SlowTrafficSegment? = null

            val startIndex =
                (if (legIndex == currentLeg) firstGeometryIndex else 0).coerceIn(0, geometry.size)
            // Accumulate distances for geometry points skipped within the current leg.
            for (i in 0 until startIndex) {
                accumulatedDistance += geometry.distance(i)
            }

            for (geometryIndex in startIndex until geometry.size) {
                if (result.size >= segmentsLimit) return@withContext result

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
                            legIndex = legIndex,
                            geometryRange = geometryIndex..geometryIndex,
                            distanceFromRouteStartMeters = accumulatedDistance,
                            lengthMeters = 0.0,
                            freeFlowDuration = Duration.ZERO,
                            duration = Duration.ZERO,
                        )
                    }
                    currentSegment = currentSegment.updateBy(geometry, geometryIndex)
                }
                accumulatedDistance += geometry.distance(geometryIndex)
            }
            // Last congestion segment, if any, at the end of the leg
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
            legFreeFlowSpeed != null && legFreeFlowSpeed > 0 -> {
                // Speed is km/h, but distance is in the meters.
                // Applying conversion of the speed from km/h -> m/s
                geometry.distance(geometryIndex) * KM_PER_H_TO_M_PER_SEC_RATE / legFreeFlowSpeed
            }

            0.0 < this.lengthMeters -> {
                // Adding average duration based on the previous geometry
                val avgTimePerMeter =
                    this.freeFlowDuration.inWholeSeconds.toDouble() / this.lengthMeters
                geometry.distance(geometryIndex) * avgTimePerMeter
            }

            else -> 0.0
        }

        return this.copy(
            freeFlowDuration = this.freeFlowDuration + freeFlowDurationSec.seconds,
            duration = this.duration + geometry.duration(geometryIndex),
            lengthMeters = this.lengthMeters + geometry.distance(geometryIndex),
            geometryRange = geometryRange.first..geometryIndex,
        ).apply {
            // Each geometry consists of 2 points
            if (_points.isEmpty()) {
                _points.add(geometry.point(geometryIndex))
            }
            _points.add(geometry.point(geometryIndex + 1))
        }
    }

    private fun List<IntRange>.rangeOf(congestion: Int?): IntRange? {
        if (congestion == null) return null
        for (i in indices) {
            if (this[i].contains(congestion)) return this[i]
        }
        return null
    }

    private companion object {

        // Calc time with seconds:
        // dist_m / speed_km_h = dist_m / (speed_km_h * 1000 / 3600) = dist_m * 3.6 / speed_km_h = duration_sec
        private const val KM_PER_H_TO_M_PER_SEC_RATE = 3.6
    }
}

/**
 * A wrapper around the annotation lists of [RouteLeg] for code readability.
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
    fun point(index: Int): Point = points[index.coerceIn(points.indices)]

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
