package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.MultiPoint
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
        val pointCurves = Array(curveCount) { mutableListOf<ReplayRouteLocation>() }

        for (routeIndex in 1 until route.lastIndex) {
            val pointCurve = locationCurve(
                route[routeIndex - 1].point,
                route[routeIndex],
                route[routeIndex + 1].point
            )
            if (pointCurve.size > 1) {
                val curveDistance = TurfMeasurement.length(pointCurve, TurfConstants.UNIT_METERS)
                pointCurves[routeIndex - 1].addAll(
                    pointCurve.mapIndexed { curveIndex, point ->
                        val curvePosition = curveIndex / pointCurve.lastIndex.toDouble()
                        val routeLocation = route[routeIndex]
                        ReplayRouteLocation(routeLocation.routeIndex, point).also { replayRouteLocation ->
                            replayRouteLocation.curvePosition = curvePosition
                            replayRouteLocation.speedMps = routeLocation.speedMps
                            replayRouteLocation.distance = curveDistance * (1.0 - curvePosition)
                        }
                    }
                )
            } else {
                pointCurves[routeIndex - 1].add(route[routeIndex])
            }
        }

        val bentRoute = mutableListOf<ReplayRouteLocation>()
        bentRoute.add(route.first())
        pointCurves.forEach { pointCurve ->
            bentRoute.last().distance = TurfMeasurement.distance(
                bentRoute.last().point,
                pointCurve.first().point,
                TurfConstants.UNIT_METERS
            )
            pointCurve.forEach { item -> bentRoute.add(item) }
        }
        bentRoute.last().distance = TurfMeasurement.distance(
            bentRoute.last().point,
            route.last().point,
            TurfConstants.UNIT_METERS
        )
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

        val startLength = min(startDistance * 0.40, MAX_CURVE_LENGTH_METERS)
        val startMultiplier = 1.0 - (startLength / startDistance)
        val startPoint = pointAlong(start, mid, startMultiplier * startDistance)

        val endLength = min(endDistance * 0.40, MAX_CURVE_LENGTH_METERS)
        val endMultiplier = (endLength / endDistance)
        val endPoint = pointAlong(mid, end, endMultiplier * endDistance)

        val granularity = if (midReplayLocation.speedMps < 5.0) 7 else 5
        return curve(granularity, startPoint, mid, endPoint)
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

        println("startToMidDistance: $startToMidDistance midToEndDistance: $midToEndDistance granularity: $granularity")
        println("curve: ${MultiPoint.fromLngLats(curvePoints).toJson()}")
        return curvePoints
    }

    private fun pointAlong(start: Point, end: Point, distance: Double): Point {
        val direction = TurfMeasurement.bearing(end, start) - 180.0
        return TurfMeasurement.destination(start, distance, direction, TurfConstants.UNIT_METERS)
    }
}
