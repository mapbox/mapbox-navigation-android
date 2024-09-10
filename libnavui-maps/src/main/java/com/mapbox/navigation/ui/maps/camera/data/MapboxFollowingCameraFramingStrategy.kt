package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc

/**
 * Default [FollowingCameraFramingStrategy] used to calculate points to be framed for
 * the following camera.
 */
internal object MapboxFollowingCameraFramingStrategy : FollowingCameraFramingStrategy {

    private const val LOG_CATEGORY = "MapboxFollowingFrameProcessor"
    private const val maxAngleDifferenceForGeometrySlicing: Double = 100.0

    /**
     * Returns points to be framed on the remainder of the current step.
     */
    override fun getPointsToFrameOnCurrentStep(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        averageIntersectionDistancesOnRoute: List<List<Double>>,
    ): List<Point> = ifNonNull(
        routeProgress.currentLegProgress,
        routeProgress.currentLegProgress?.currentStepProgress,
    ) { currentLegProgress, currentStepProgress ->
        followingFrameOptions.intersectionDensityCalculation.run {
            getPointsToFrameOnCurrentStep(
                intersectionDensityCalculationEnabled = enabled,
                intersectionDensityAverageDistanceMultiplier = averageDistanceMultiplier,
                averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
                currentLegProgress = currentLegProgress,
                currentStepProgress = currentStepProgress,
            )
        }
    } ?: emptyList()

    /**
     * Finds the points to frame after maneuver based on preprocessed data and settings.
     */
    override fun getPointsToFrameAfterCurrentManeuver(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        postManeuverFramingPoints: List<List<List<Point>>>,
    ): List<Point> = ifNonNull(
        routeProgress.currentLegProgress,
        routeProgress.currentLegProgress?.currentStepProgress,
    ) { currentLegProgress, currentStepProgress ->
        followingFrameOptions.frameGeometryAfterManeuver.run {
            getPointsToFrameAfterCurrentManeuver(
                frameGeometryAfterManeuverEnabled = enabled,
                generatedPostManeuverFramingPoints = postManeuverFramingPoints,
                currentLegProgress = currentLegProgress,
                currentStepProgress = currentStepProgress,
            )
        }
    } ?: emptyList()

    private fun getPointsToFrameOnCurrentStep(
        intersectionDensityCalculationEnabled: Boolean,
        intersectionDensityAverageDistanceMultiplier: Double,
        averageIntersectionDistancesOnRoute: List<List<Double>>,
        currentLegProgress: RouteLegProgress,
        currentStepProgress: RouteStepProgress,
    ): List<Point> {
        var distanceTraveledOnStepKM =
            currentStepProgress.distanceTraveled.metersToKilometers().coerceAtLeast(0.0)
        val fullDistanceOfCurrentStepKM =
            (currentStepProgress.distanceRemaining + currentStepProgress.distanceTraveled)
                .metersToKilometers().coerceAtLeast(0.0)
        if (distanceTraveledOnStepKM > fullDistanceOfCurrentStepKM) {
            distanceTraveledOnStepKM = 0.0
        }

        var lookaheadDistanceForZoom = fullDistanceOfCurrentStepKM

        if (intersectionDensityCalculationEnabled &&
            averageIntersectionDistancesOnRoute.isNotEmpty()
        ) {
            val lookaheadInKM = averageIntersectionDistancesOnRoute
                .get(currentLegProgress.legIndex)
                .get(currentStepProgress.stepIndex)
                .metersToKilometers()
            lookaheadDistanceForZoom = distanceTraveledOnStepKM +
                (lookaheadInKM * intersectionDensityAverageDistanceMultiplier)
        }

        return try {
            val currentStepFullPoints = currentStepProgress.stepPoints ?: emptyList()
            // todo bottom slice might not be needed since we always append the user location
            val lineSliceCoordinatesForLookaheadDistance = TurfMisc.lineSliceAlong(
                LineString.fromLngLats(currentStepFullPoints),
                distanceTraveledOnStepKM,
                lookaheadDistanceForZoom,
                TurfConstants.UNIT_KILOMETERS,
            ).coordinates()
            ViewportDataSourceProcessor.slicePointsAtAngle(
                lineSliceCoordinatesForLookaheadDistance,
                maxAngleDifferenceForGeometrySlicing,
            )
        } catch (e: TurfException) {
            logE(e.message.toString(), LOG_CATEGORY)
            emptyList()
        }
    }

    private fun getPointsToFrameAfterCurrentManeuver(
        frameGeometryAfterManeuverEnabled: Boolean,
        generatedPostManeuverFramingPoints: List<List<List<Point>>>,
        currentLegProgress: RouteLegProgress,
        currentStepProgress: RouteStepProgress,
    ): List<Point> {
        return if (
            frameGeometryAfterManeuverEnabled &&
            generatedPostManeuverFramingPoints.isNotEmpty()
        ) {
            generatedPostManeuverFramingPoints
                .get(currentLegProgress.legIndex)
                .get(currentStepProgress.stepIndex)
        } else {
            emptyList()
        }
    }
}
