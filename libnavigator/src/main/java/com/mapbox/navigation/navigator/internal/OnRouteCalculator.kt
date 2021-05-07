package com.mapbox.navigation.navigator.internal

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OnRouteCalculator {

    /**
     * Given a location, returns true if it is on route.
     */
    fun isOnRoute(point: Point, route: List<Point>): Boolean {
        val indexOfNextStop = indexOfNextLocation(
            point,
            route
        )
        LoggerProvider.logger.i(
            tag = Tag("RouteComparator"),
            msg = Message("isOnRoute ${point.latitude()},${point.longitude()} $indexOfNextStop")
        )
        return indexOfNextStop != null
    }

    /**
     * Find the index of a location on route. The index will represent an upcoming or equal
     * location. If a location is not found, return null.
     */
    fun indexOfNextLocation(
        point: Point,
        directions: List<Point>
    ): Int? {
        var minIndex: Int? = null
        var minDistance = OFF_ROUTE_THRESHOLD_METERS
        for (i in 1 until directions.size) {
            val distanceToRoad = distanceToSegment(
                directions[i - 1],
                point,
                directions[i]
            )
            if (distanceToRoad != null && abs(distanceToRoad) < minDistance) {
                minIndex = i
                minDistance = distanceToRoad
            }
        }
        return minIndex
    }

    /**
     * Given three points on a road segment. This will return the turn perpendicular distance
     * of the middle point to the road. If the road direction is being reversed, return null.
     * The sign of the distance says if it is turning one direction vs another.
     */
    fun distanceToSegment(segmentStart: Point, middlePoint: Point, segmentEnd: Point): Double? {
        val p0: DoubleArray = cartesian(segmentStart)
        val p1: DoubleArray = cartesian(segmentEnd)
        val c: DoubleArray = cartesian(middlePoint)
        val v0: DoubleArray = normalize(vector(p0, p1))
        val v1: DoubleArray = vector(p0, c)
        val directionVector = crossProduct(v0, v1)
        val distance = dotProduct(gravity(p0), directionVector)
        val isUTurn = isNaN(v0) || dotProduct(v0, vector(c, p1)) < 0
        return if (!isUTurn) {
            distance
        } else {
            null
        }
    }

    /**
     * Vector3 math so we can calculate the distance to a road segment.
     */

    private fun gravity(vector3: DoubleArray): DoubleArray {
        return normalize(vector(OnRouteCalculator.EARTH_CENTER, vector3))
    }

    private fun cartesian(point: Point): DoubleArray {
        val latitudeRadians = Math.toRadians(point.latitude())
        val longitudeRadians = Math.toRadians(point.longitude())
        return doubleArrayOf(
            EARTH_RADIUS * cos(latitudeRadians) * cos(longitudeRadians),
            EARTH_RADIUS * cos(latitudeRadians) * sin(longitudeRadians),
            EARTH_RADIUS * sin(latitudeRadians)
        )
    }

    private fun isNaN(vector3: DoubleArray): Boolean {
        return vector3[0].isNaN() ||
            vector3[1].isNaN() ||
            vector3[2].isNaN()
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
        return lhsVector3[0].times(rhsVector3[0])
            .plus(lhsVector3[1].times(rhsVector3[1]))
            .plus(lhsVector3[2].times(rhsVector3[2]))
    }

    private fun crossProduct(lhs: DoubleArray, rhs: DoubleArray): DoubleArray {
        return doubleArrayOf(
            lhs[1] * rhs[2] - lhs[2] * rhs[1],
            lhs[2] * rhs[0] - lhs[0] * rhs[2],
            lhs[0] * rhs[1] - lhs[1] * rhs[0]
        )
    }

    companion object {
        val EARTH_CENTER: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
        const val EARTH_RADIUS = 6378137
        const val OFF_ROUTE_THRESHOLD_METERS = 1.0
    }
}
