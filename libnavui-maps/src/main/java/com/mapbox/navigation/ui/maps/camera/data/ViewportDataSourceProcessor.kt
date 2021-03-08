package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object ViewportDataSourceProcessor {
    fun processRouteInfo(route: DirectionsRoute): List<List<List<Point>>> {
        return route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    PolylineUtils.decode(geometry, Constants.PRECISION_6).toList()
                } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }

    fun processRouteForPostManeuverFramingGeometry(
        distanceToCoalesceCompoundManeuvers: Double,
        defaultDistanceToFrameAfterManeuver: Double,
        route: DirectionsRoute
    ): List<List<List<Point>>> {
        return route.legs()?.map { routeLeg ->
            routeLeg.steps()?.mapIndexed<LegStep?, List<Point>> { index, _ ->
                val stepsAfter: List<LegStep> = routeLeg.steps()?.drop(index + 1) ?: emptyList()
                val compoundManeuverGeometryList =
                    stepsAfter
                        .takeWhile { it.distance() <= distanceToCoalesceCompoundManeuvers }
                        .map { it.geometry() }
                val compoundManeuverGeometryPoints =
                    compoundManeuverGeometryList.filterNotNull()
                        .map<String, List<Point>> {
                            PolylineUtils.decode(it, Constants.PRECISION_6).toList()
                        }
                        .flatten()

                var defaultGeometryPointsAfterManeuver = emptyList<Point>()
                if (compoundManeuverGeometryList.size < stepsAfter.size) {
                    defaultGeometryPointsAfterManeuver = TurfMisc.lineSliceAlong(
                        LineString.fromPolyline(
                            stepsAfter[compoundManeuverGeometryList.size].geometry() ?: "",
                            Constants.PRECISION_6
                        ), 0.0, defaultDistanceToFrameAfterManeuver, TurfConstants.UNIT_METERS
                    ).coordinates()
                }
                compoundManeuverGeometryPoints + defaultGeometryPointsAfterManeuver
            } ?: emptyList()
        } ?: emptyList()
    }

    fun processRouteIntersections(
        minimumMetersForIntersectionDensity: Double,
        route: DirectionsRoute
    ): List<List<Double>> {
        return route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    val stepPoints = PolylineUtils.decode(geometry, Constants.PRECISION_6).toList()
                    val intersectionLocations =
                        legStep.intersections()?.map { it.location() } ?: emptyList()
                    val list: MutableList<Point> = ArrayList()
                    list.add(stepPoints.first())
                    list.addAll(intersectionLocations)
                    list.add(stepPoints.last())
                    val comparisonList = list.toMutableList()
                    list.removeFirst()
                    val intersectionDistances = list.mapIndexed { index, point ->
                        TurfMeasurement.distance(point, comparisonList[index]) * 1000.0
                    }
                    val filteredIntersectionDistances =
                        intersectionDistances.filter { it > minimumMetersForIntersectionDensity }
                    if (filteredIntersectionDistances.isNotEmpty()) {
                        filteredIntersectionDistances.reduce { acc, next -> acc + next } / filteredIntersectionDistances.size
                    } else {
                        minimumMetersForIntersectionDensity
                    }
                } ?: 0.0
            } ?: emptyList()
        } ?: emptyList()
    }

    fun getPitchForDistanceRemainingOnStep(
        distanceFromManeuverToBeginPitchChange: Double,
        distanceFromManeuverToEndPitchChange: Double,
        distanceRemaining: Float,
        minPitch: Double = 0.0,
        maxPitch: Double = 60.0
    ): Double {
        val denominator =
            distanceFromManeuverToBeginPitchChange - distanceFromManeuverToEndPitchChange
        if (denominator > 0) {
            val percentage = 1.0 - min(
                max(
                    (distanceFromManeuverToBeginPitchChange - distanceRemaining) / denominator,
                    0.0
                ),
                1.0
            )
            return minPitch + (maxPitch - minPitch) * percentage
        }
        return 0.0
    }

    fun getPitchPercentage(currentPitch: Double = 0.0, maxPitch: Double = 60.0): Double {
        if (maxPitch == 0.0) return 0.0
        return (currentPitch / maxPitch).coerceIn(0.0, 1.0)
    }

    fun getAnchorPointFromPitchPercentage(
        pitchPercentage: Double,
        mapSize: Size,
        padding: EdgeInsets
    ): ScreenCoordinate {
        val centerInsidePaddingX =
            ((mapSize.width - padding.left - padding.right) / 2.0) + padding.left
        val centerInsidePaddingY =
            ((mapSize.height - padding.top - padding.bottom) / 2.0) + padding.top
        val anchorPointX = centerInsidePaddingX
        val anchorPointY =
            ((mapSize.height - padding.bottom - centerInsidePaddingY) * pitchPercentage) + centerInsidePaddingY
        return ScreenCoordinate(anchorPointX, anchorPointY)
    }

    fun getEdgeInsetsFromPoint(mapSize: Size, screenPoint: ScreenCoordinate? = null): EdgeInsets {
        val point = screenPoint ?: ScreenCoordinate(
            mapSize.width.toDouble() / 2.0,
            mapSize.height.toDouble() / 2.0
        )
        return EdgeInsets(point.y, point.x, mapSize.height - point.y, mapSize.width - point.x)
    }

    fun getBearingForMap(
        bearingDiffMax: Double,
        currentMapCameraBearing: Double,
        vehicleBearing: Double,
        pointsForBearing: List<Point>
    ): Double {
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

    fun getMapCenterCoordinateFromPitchPercentage(
        pitchPercentage: Double,
        vehicleLocation: Point,
        framingGeometryCentroid: Point
    ): Point {
        val distance = TurfMeasurement.distance(framingGeometryCentroid, vehicleLocation)
        return TurfMeasurement.along(
            listOf(framingGeometryCentroid, vehicleLocation),
            distance * pitchPercentage,
            TurfConstants.UNIT_KILOMETERS
        )
    }
}
