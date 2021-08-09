package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlin.math.abs

internal object ViewportDataSourceProcessor {

    private const val TAG = "MbxViewportDataSource"

    private const val maxAngleDifferenceForGeometrySlicing: Double = 100.0

    /**
     * Returns complete route points in nested arrays of points for all steps in all legs arranged as \[legs]\[steps]\[points].
     */
    fun processRoutePoints(route: DirectionsRoute): List<List<List<Point>>> {
        return route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    PolylineUtils.decode(geometry, route.precision()).toList()
                } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }

    /**
     * Returns a list of points that trails each maneuver based on the method arguments.
     *
     * Those points can later be used as additional input for framing.
     */
    fun processRouteForPostManeuverFramingGeometry(
        enabled: Boolean,
        distanceToCoalesceCompoundManeuvers: Double,
        distanceToFrameAfterManeuver: Double,
        route: DirectionsRoute,
        completeRoutePoints: List<List<List<Point>>>
    ): List<List<List<Point>>> {
        if (!enabled) {
            return emptyList()
        }

        return route.legs()?.mapIndexed { legIndex, leg ->
            leg.steps()?.mapIndexed { stepIndex, _ ->
                val legSteps = leg.steps() ?: emptyList()
                val stepsPoints = completeRoutePoints[legIndex]

                if (legSteps.size != stepsPoints.size) {
                    LoggerProvider.logger.e(
                        Tag(TAG),
                        Message("Unable to calculate geometry after maneuvers. Invalid route.")
                    )
                    return emptyList()
                }

                val compoundManeuverGeometryPoints = mutableListOf<Point>()
                var lastCompoundStepIndex = stepIndex
                for (i in stepIndex + 1 until stepsPoints.size) {
                    if (legSteps[i].distance() <= distanceToCoalesceCompoundManeuvers) {
                        compoundManeuverGeometryPoints.addAll(stepsPoints[i])
                        lastCompoundStepIndex = i
                    } else {
                        break
                    }
                }

                var defaultGeometryPointsAfterManeuver = emptyList<Point>()
                if (lastCompoundStepIndex < stepsPoints.size - 1) {
                    defaultGeometryPointsAfterManeuver = TurfMisc.lineSliceAlong(
                        LineString.fromLngLats(stepsPoints[lastCompoundStepIndex + 1]),
                        0.0,
                        distanceToFrameAfterManeuver,
                        TurfConstants.UNIT_METERS
                    ).coordinates()
                }
                compoundManeuverGeometryPoints + defaultGeometryPointsAfterManeuver
            } ?: emptyList()
        } ?: emptyList()
    }

    /**
     * Processes points for framing for each step based on the intersections density on that step,
     * instead of taking all the step points.
     */
    fun processRouteIntersections(
        enabled: Boolean,
        minimumMetersForIntersectionDensity: Double,
        route: DirectionsRoute,
        completeRoutePoints: List<List<List<Point>>>
    ): List<List<Double>> {
        if (!enabled) {
            return emptyList()
        }
        return route.legs()?.mapIndexed { legIndex, leg ->
            leg.steps()?.mapIndexed { stepIndex, step ->
                val stepPoints = completeRoutePoints[legIndex][stepIndex]
                val intersectionLocations =
                    step.intersections()?.map { it.location() } ?: emptyList()
                val list: MutableList<Point> = ArrayList()
                list.add(stepPoints.first())
                list.addAll(intersectionLocations)
                list.add(stepPoints.last())
                val comparisonList = list.toMutableList()
                list.removeFirst()
                val intersectionDistances = list.mapIndexed { index, point ->
                    TurfMeasurement.distance(point, comparisonList[index]).kilometersToMeters()
                }
                val filteredIntersectionDistances =
                    intersectionDistances.filter { it > minimumMetersForIntersectionDensity }
                if (filteredIntersectionDistances.isNotEmpty()) {
                    filteredIntersectionDistances
                        .reduce { acc, next -> acc + next }
                        .div(filteredIntersectionDistances.size)
                } else {
                    minimumMetersForIntersectionDensity
                }
            } ?: emptyList()
        } ?: emptyList()
    }

    fun simplifyCompleteRoutePoints(
        enabled: Boolean,
        simplificationFactor: Int,
        completeRoutePoints: List<List<List<Point>>>
    ): List<List<List<Point>>> {
        if (!enabled) {
            return completeRoutePoints
        }

        if (simplificationFactor <= 0) {
            LoggerProvider.logger.e(
                Tag(TAG),
                Message("overview simplification factor should be a positive integer")
            )
            return completeRoutePoints
        }

        return completeRoutePoints.map { steps ->
            steps.map { stepPoints ->
                stepPoints.filterIndexed { index, _ ->
                    index % simplificationFactor == 0 || index == stepPoints.size - 1
                }
            }
        }
    }

    /**
     * Returns points to be framed on the remainder of the current step.
     */
    fun getPointsToFrameOnCurrentStep(
        intersectionDensityCalculationEnabled: Boolean,
        intersectionDensityAverageDistanceMultiplier: Double,
        averageIntersectionDistancesOnRoute: List<List<Double>>,
        currentLegProgress: RouteLegProgress,
        currentStepProgress: RouteStepProgress
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
                TurfConstants.UNIT_KILOMETERS
            ).coordinates()
            slicePointsAtAngle(
                lineSliceCoordinatesForLookaheadDistance,
                maxAngleDifferenceForGeometrySlicing
            )
        } catch (e: TurfException) {
            LoggerProvider.logger.e(Tag(TAG), Message(e.message.toString()))
            emptyList()
        }
    }

    /**
     * Returns route geometry sliced at the point where it exceeds a certain angle difference from
     * the first edge's bearing.
     */
    fun slicePointsAtAngle(
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
                val rotationDiff = shortestRotationDiff(thisEdgeBearing, firstEdgeBearing)
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

    /**
     * Returns the pitch based on route progress and settings.
     */
    fun getPitchFallbackFromRouteProgress(
        pitchNearManeuversEnabled: Boolean,
        triggerDistanceForPitchZero: Double,
        defaultPitch: Double,
        distanceRemainingOnStep: Float
    ): Double {
        return if (
            pitchNearManeuversEnabled &&
            distanceRemainingOnStep <= triggerDistanceForPitchZero
        ) {
            MapboxNavigationViewportDataSource.ZERO_PITCH
        } else {
            defaultPitch
        }
    }

    /**
     * Finds the points to frame after maneuver based on preprocessed data and settings.
     */
    fun getPointsToFrameAfterCurrentManeuver(
        frameGeometryAfterManeuverEnabled: Boolean,
        generatedPostManeuverFramingPoints: List<List<List<Point>>>,
        currentLegProgress: RouteLegProgress,
        currentStepProgress: RouteStepProgress
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

    /**
     * Returns all remaining points on the route for overview purposes.
     */
    fun getRemainingPointsOnRoute(
        simplifiedCompleteRoutePoints: List<List<List<Point>>>,
        pointsToFrameOnCurrentStep: List<Point>,
        currentLegProgress: RouteLegProgress,
        currentStepProgress: RouteStepProgress
    ): List<Point> {
        val currentLegPoints = if (simplifiedCompleteRoutePoints.isNotEmpty()) {
            simplifiedCompleteRoutePoints[currentLegProgress.legIndex]
        } else {
            emptyList()
        }
        val remainingStepsAfterCurrentStep =
            if (currentStepProgress.stepIndex < currentLegPoints.size) {
                currentLegPoints.slice(
                    currentStepProgress.stepIndex + 1 until currentLegPoints.size - 1
                )
            } else {
                emptyList()
            }
        val remainingPointsAfterCurrentStep = remainingStepsAfterCurrentStep.flatten()
        return listOf(
            pointsToFrameOnCurrentStep,
            remainingPointsAfterCurrentStep
        ).flatten()
    }

    /**
     * Returns a padding value by transforming a user-provided padding to a single-pixel padding value.
     *
     * That single-pixel anchor sits on the bottom edge of the provided padding, centered horizontally.
     */
    fun getMapAnchoredPaddingFromUserPadding(mapSize: Size, padding: EdgeInsets): EdgeInsets {
        val verticalRange = 0f..mapSize.height
        val horizontalRange = 0f..mapSize.width
        padding.run {
            if (!verticalRange.contains(top) ||
                !verticalRange.contains(bottom) ||
                !horizontalRange.contains(left) ||
                !horizontalRange.contains(right) ||
                top + bottom > mapSize.height ||
                left + right > mapSize.width
            ) {
                LoggerProvider.logger.e(
                    Tag(TAG),
                    Message(
                        """Provided following padding does fit the map size:
                        |mapSize: $mapSize
                        |padding: $padding
                    """.trimMargin()
                    )
                )
                return EdgeInsets(
                    mapSize.height / 2.0,
                    mapSize.width / 2.0,
                    mapSize.height / 2.0,
                    mapSize.width / 2.0,
                )
            }
        }

        val anchorPointX =
            ((mapSize.width - padding.left - padding.right) / 2.0) + padding.left
        val centerInsidePaddingY =
            ((mapSize.height - padding.top - padding.bottom) / 2.0) + padding.top
        val anchorPointY =
            ((mapSize.height - padding.bottom - centerInsidePaddingY)).plus(centerInsidePaddingY)

        return EdgeInsets(
            anchorPointY,
            anchorPointX,
            mapSize.height - anchorPointY,
            mapSize.width - anchorPointX
        )
    }

    fun getScreenBoxForFraming(mapSize: Size, padding: EdgeInsets): ScreenBox {
        val topLeft = ScreenCoordinate(
            padding.left,
            padding.top
        )
        val bottomRight = ScreenCoordinate(
            mapSize.width - padding.right,
            mapSize.height - padding.bottom
        )
        return ScreenBox(
            topLeft,
            bottomRight
        )
    }

    /**
     * Returns smoothed bearing value based on the vehicle bearing and heading to the maneuver.
     */
    fun getSmootherBearingForMap(
        enabled: Boolean,
        bearingDiffMax: Double,
        currentMapCameraBearing: Double,
        vehicleBearing: Double,
        pointsForBearing: List<Point>
    ): Double {
        if (!enabled) {
            return vehicleBearing
        }
        var output = vehicleBearing
        if (pointsForBearing.size > 1) {
            val bearingFromPointsFirstToLast =
                TurfMeasurement.bearing(pointsForBearing.first(), pointsForBearing.last())
            val bearingDiff = shortestRotationDiff(bearingFromPointsFirstToLast, vehicleBearing)
            if (abs(bearingDiff) > bearingDiffMax) {
                val diffDirection = if (bearingDiff < 0.0) -1.0 else 1.0
                output += bearingDiffMax * diffDirection
            } else {
                output = bearingFromPointsFirstToLast
            }
        }
        return currentMapCameraBearing + shortestRotationDiff(output, currentMapCameraBearing)
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
}

private fun DirectionsRoute.precision() =
    if (this.routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
        Constants.PRECISION_5
    } else {
        Constants.PRECISION_6
    }

private fun Double.metersToKilometers() = this / 1000.0

private fun Float.metersToKilometers() = this / 1000.0

private fun Double.kilometersToMeters() = this * 1000.0
