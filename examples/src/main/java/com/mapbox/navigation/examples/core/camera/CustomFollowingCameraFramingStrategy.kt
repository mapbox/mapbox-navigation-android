package com.mapbox.navigation.examples.core.camera

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.camera.data.FollowingCameraFramingStrategy
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlin.math.abs

/**
 * An example [FollowingCameraFramingStrategy] that frames route either only X meters ahead or
 * up until the next maneuver - whichever is closest.
 */
internal class CustomFollowingCameraFramingStrategy(
    lookaheadInMeters: Double = 500.0
) : FollowingCameraFramingStrategy {

    private val lookaheadInKM = lookaheadInMeters.metersToKilometers()

    override fun getPointsToFrameOnCurrentStep(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        averageIntersectionDistancesOnRoute: List<List<Double>>
    ): List<Point> {
        return ifNonNull(
            routeProgress.currentLegProgress,
            routeProgress.currentLegProgress?.currentStepProgress
        ) { currentLegProgress, currentStepProgress ->
            var distanceTraveledOnStepKM =
                currentStepProgress.distanceTraveled.metersToKilometers().coerceAtLeast(0.0)
            val fullDistanceOfCurrentStepKM =
                (currentStepProgress.distanceRemaining + currentStepProgress.distanceTraveled)
                    .metersToKilometers().coerceAtLeast(0.0)
            if (distanceTraveledOnStepKM > fullDistanceOfCurrentStepKM) {
                distanceTraveledOnStepKM = 0.0
            }

            val lookaheadDistanceForZoom = distanceTraveledOnStepKM + lookaheadInKM

            return try {
                val currentStepFullPoints = currentStepProgress.stepPoints ?: emptyList()
                val lineSliceCoordinatesForLookaheadDistance = TurfMisc.lineSliceAlong(
                    LineString.fromLngLats(currentStepFullPoints),
                    distanceTraveledOnStepKM,
                    lookaheadDistanceForZoom,
                    TurfConstants.UNIT_KILOMETERS
                ).coordinates()
                slicePointsAtAngle(
                    lineSliceCoordinatesForLookaheadDistance,
                    maxAngleDifferenceForGeometrySlicing
                )
            } catch (e: TurfException) {
                logE(e.message.toString(), LOG_CATEGORY)
                emptyList()
            }
        } ?: emptyList()
    }

    override fun getPointsToFrameAfterCurrentManeuver(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        postManeuverFramingPoints: List<List<List<Point>>>
    ): List<Point> =
        FollowingCameraFramingStrategy.Default.getPointsToFrameAfterCurrentManeuver(
            routeProgress = routeProgress,
            followingFrameOptions = followingFrameOptions,
            postManeuverFramingPoints = postManeuverFramingPoints
        )

    private fun slicePointsAtAngle(
        points: List<Point>,
        maxAngleDifference: Double
    ): List<Point> {
        if (points.size < 2) return points
        val outputCoordinates: MutableList<Point> = emptyList<Point>().toMutableList()
        val firstEdgeBearing = TurfMeasurement.bearing(points[0], points[1])
        outputCoordinates.add(points[0])
        for (index in points.indices) {
            if (index == 0) {
                continue
            }
            val coord = points[index - 1].let {
                val thisEdgeBearing = TurfMeasurement.bearing(it, points[index])
                val rotationDiff = shortestRotationDiff(
                    thisEdgeBearing,
                    firstEdgeBearing
                )
                if (abs(rotationDiff) < maxAngleDifference) {
                    points[index]
                } else {
                    null
                }
            }
            if (coord != null) {
                outputCoordinates.add(coord)
            } else {
                break
            }
        }
        return outputCoordinates
    }

    private fun shortestRotationDiff(angle: Double, anchorAngle: Double): Double {
        if (angle.isNaN() || anchorAngle.isNaN()) {
            return 0.0
        }
        val rawAngleDiff = angle - anchorAngle
        return wrap(rawAngleDiff, -180.0, 180.0)
    }

    private fun wrap(angle: Double, min: Double, max: Double): Double {
        val d = max - min
        return ((((angle - min) % d) + d) % d) + min
    }

    private fun Double.metersToKilometers() = this / 1000.0
    private fun Float.metersToKilometers() = this / 1000.0

    private val LOG_CATEGORY = "CustomFollowingFrameCalculationStrategy"
    private val maxAngleDifferenceForGeometrySlicing: Double = 100.0
}
