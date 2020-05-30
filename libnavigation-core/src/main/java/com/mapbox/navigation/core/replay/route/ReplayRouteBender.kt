package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.Collections
import kotlin.math.abs
import kotlin.math.min

internal class ReplayRouteBender {

    companion object {
        const val MAX_BEARING_DELTA = 40.0
        const val MAX_CURVE_LENGTH_METERS = 5.0
    }

    fun maxBearingsDelta(points: List<Point>): Double {
        var maxBearingDelta = 0.0
        var previousBearing = TurfMeasurement.bearing(points[0], points[1])
        for (i in 1..points.lastIndex) {
            val bearing = TurfMeasurement.bearing(points[i - 1], points[i])
            val delta = abs(bearing-previousBearing)
            maxBearingDelta = if (delta > maxBearingDelta) delta else maxBearingDelta
            previousBearing = bearing
        }
        return maxBearingDelta
    }

    fun bendRoute(
        route: List<ReplayRouteLocation>
    ): List<ReplayRouteLocation> {
        val curveCount = route.size - 2
        val pointCurves = Array(curveCount) {
            mutableListOf<ReplayRouteLocation>()
        }

        for (i in 1 until route.lastIndex) {
            val pointCurve = locationCurve(
                route[i - 1].point,
                route[i],
                route[i + 1].point
            )
            pointCurves[i - 1].addAll(pointCurve.map { ReplayRouteLocation(i, it) })
        }

        val bentRoute = mutableListOf<ReplayRouteLocation>()
        bentRoute.add(route.first())
        pointCurves.forEach { it.forEach { item -> bentRoute.add(item) } }
        bentRoute.add(route.last())

        return bentRoute
    }

    private fun locationCurve(
        start: Point,
        midReplayLocation: ReplayRouteLocation,
        end: Point
    ): List<Point> {
        val mid = midReplayLocation.point
        val startBearing = TurfMeasurement.bearing(start, mid)
        val endBearing = TurfMeasurement.bearing(mid, end)
        val deltaBearing = abs(endBearing - startBearing)
        if (deltaBearing < MAX_BEARING_DELTA) {
            return Collections.singletonList(mid)
        }

        val startDistance = TurfMeasurement.distance(start, mid, TurfConstants.UNIT_METERS)
        val endDistance = TurfMeasurement.distance(mid, end, TurfConstants.UNIT_METERS)

        val startLength = min(startDistance - 0.1, MAX_CURVE_LENGTH_METERS)
        val startMultiplier = 1.0 - (startLength / startDistance)
        val startPoint = pointAlong(start, mid, startMultiplier * startDistance)

        val endLength = min(endDistance - 0.1, MAX_CURVE_LENGTH_METERS)
        val endMultiplier = (endLength / endDistance)
        val endPoint = pointAlong(mid, end, endMultiplier * endDistance)

        val granularity = if (midReplayLocation.speedMps < 5.0) 7 else 5
        val curve = curve(granularity, startPoint, mid, endPoint)

        val curveLength = TurfMeasurement.length(curve, TurfConstants.UNIT_METERS)
        println("locationCurve $curveLength ${midReplayLocation.speedMps} $deltaBearing")

        return curve
    }

    private fun curve(
        points: Int,
        startPoint: Point,
        mid: Point,
        endPoint: Point
    ): List<Point> {
        val startToMidDistance = TurfMeasurement.distance(
            startPoint,
            mid,
            TurfConstants.UNIT_METERS
        )
        val midToEndDistance = TurfMeasurement.distance(
            mid,
            endPoint,
            TurfConstants.UNIT_METERS
        )

        val granularity = points - 1
        val curvePoints = mutableListOf<Point>()
        for (i in 0..granularity) {
            val lerpMultiplier = (i.toDouble() / granularity)
            val fromLerpPoint = pointAlong(
                startPoint,
                mid,
                lerpMultiplier * startToMidDistance
            )
            val toLerpPoint = pointAlong(mid, endPoint, lerpMultiplier * midToEndDistance)
            val lerpDistance = TurfMeasurement.distance(
                fromLerpPoint,
                toLerpPoint,
                TurfConstants.UNIT_METERS
            )
            val curvePoint = pointAlong(fromLerpPoint, toLerpPoint, lerpMultiplier * lerpDistance)
            curvePoints.add(curvePoint)
        }

        return curvePoints
    }

    private fun pointAlong(start: Point, end: Point, distance: Double): Point {
        val direction = TurfMeasurement.bearing(end, start) - 180.0
        return TurfMeasurement.destination(start, distance, direction, TurfConstants.UNIT_METERS)
    }
}
