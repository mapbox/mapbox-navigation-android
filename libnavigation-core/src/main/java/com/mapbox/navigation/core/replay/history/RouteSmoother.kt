package com.mapbox.navigation.core.replay.history

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement.EARTH_RADIUS
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class RouteSmoother {

    fun smoothRoute(coordinates: List<Point>, thresholdMeters: Double): List<Point> {
        val smoothedRoute = mutableListOf<Point>()
        if (coordinates.size <= 3) return coordinates.toList()

        smoothedRoute.addAll(coordinates)

        var i = 1
        while (i < smoothedRoute.size - 1) {
            val startPoint = smoothedRoute[i-1]
            val centerPoint = smoothedRoute[i]
            val endPoint = smoothedRoute[i+1]
            val distanceToSegment = distanceToSegment(centerPoint, startPoint, endPoint)
            if (distanceToSegment < thresholdMeters) {
                smoothedRoute.removeAt(i)
            } else {
                i++
            }
        }
        return smoothedRoute
    }

    fun distanceToSegment(location: Point, segmentStart: Point, segmentEnd: Point): Double {
        val p0: DoubleArray = toCartesian(segmentStart)
        val p1: DoubleArray = toCartesian(segmentEnd)
        val c: DoubleArray = toCartesian(location)
        val v0: DoubleArray = normalize(vector(p0, p1))
        val v1: DoubleArray = vector(p0, c)
        return magnitude(crossProduct(v0, v1))
    }

    fun toCartesian(point: Point): DoubleArray {
        val latitudeRadians = Math.toRadians(point.latitude())
        val longitudeRadians = Math.toRadians(point.longitude())
        return doubleArrayOf(
            EARTH_RADIUS * cos(latitudeRadians) * cos(longitudeRadians),
            EARTH_RADIUS * cos(latitudeRadians) * sin(longitudeRadians),
            EARTH_RADIUS * sin(latitudeRadians)
        )
    }

    fun toPoint(point3: DoubleArray): Point {
        val latitudeRadians = asin(point3[2] / EARTH_RADIUS)
        val longitudeRadians = atan2(point3[1], point3[0])
        return Point.fromLngLat(
            Math.toDegrees(longitudeRadians),
            Math.toDegrees(latitudeRadians)
        )
    }

    private fun vector(fromPoint3: DoubleArray, toPoint3: DoubleArray): DoubleArray {
        return doubleArrayOf(
            toPoint3[0] - fromPoint3[0],
            toPoint3[1] - fromPoint3[1],
            toPoint3[2] - fromPoint3[2]
        )
    }

    private fun normalize(vector3: DoubleArray): DoubleArray {
        val length: Double = magnitude(vector3)
        return doubleArrayOf(
            vector3[0] / length,
            vector3[1] / length,
            vector3[2] / length
        )
    }

    private fun magnitude(vector3: DoubleArray): Double {
        return sqrt(dotProduct(vector3, vector3))
    }

    private fun dotProduct(lhsVector3: DoubleArray, rhsVector3: DoubleArray): Double {
        return lhsVector3[0] * rhsVector3[0] + lhsVector3[1] * rhsVector3[1] + lhsVector3[2] * rhsVector3[2]
    }

    private fun crossProduct(lhs: DoubleArray, rhs: DoubleArray): DoubleArray {
        return doubleArrayOf(
            lhs[1] * rhs[2] - lhs[2] * rhs[1],
            lhs[2] * rhs[0] - lhs[0] * rhs[2],
            lhs[0] * rhs[1] - lhs[1] * rhs[0]
        )
    }
}