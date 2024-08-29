package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.pow

/**
 * This class is experimental because it is likely to move to mapbox-java.
 *
 * https://github.com/mapbox/mapbox-java/pull/1518
 *
 * @param encodedPath String representing an encoded path string
 * @param precision OSRMv4 uses 6, OSRMv5 and Google uses 5
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplayPolylineDecodeStream(
    val encodedPath: String,
    precision: Int,
) : Iterator<Point> {

    private val len = encodedPath.length

    // OSRM uses precision=6, the default Polyline spec divides by 1E5, capping at precision=5
    private val factor = 10.0.pow(precision.toDouble())

    // For speed we preallocate to an upper bound on the final length, then
    // truncate the array before returning.
    private var index = 0
    private var lat = 0
    private var lng = 0

    /**
     * Returns the current [Point] for the iterator. Every call to [next] will update the [current].
     */
    var current: Point? = null
        private set

    /**
     * Returns true if the geometry has more points.
     */
    override fun hasNext(): Boolean {
        return index < len
    }

    /**
     * Returns the next point in the geometry.
     */
    override fun next(): Point {
        var result = 1
        var shift = 0
        var temp: Int
        do {
            temp = encodedPath[index++].code - 63 - 1
            result += temp shl shift
            shift += 5
        } while (temp >= 0x1f)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        result = 1
        shift = 0
        do {
            temp = encodedPath[index++].code - 63 - 1
            result += temp shl shift
            shift += 5
        } while (temp >= 0x1f)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        return Point.fromLngLat(lng / factor, lat / factor).also { next ->
            current = next
        }
    }

    /**
     * Decode the next [minDistance] of the geometry. The input is a exclusive minimum distance
     * to decode. If the [current] is available it will be added to the beginning of the list.
     * If there are no more events to decode the list will be empty.
     *
     * @param minDistance to decode
     */
    fun decode(
        minDistance: Double,
        @TurfConstants.TurfUnitCriteria units: String = TurfConstants.UNIT_KILOMETERS,
    ): List<Point> {
        val points = mutableListOf<Point>()
        var travelled = 0.0
        current?.let { current -> if (hasNext()) points.add(current) }
        while (travelled < minDistance && hasNext()) {
            val previous = current
            val next = next()
            travelled += previous?.let { TurfMeasurement.distance(previous, next, units) } ?: 0.0
            points.add(next)
        }
        return points
    }

    /**
     * Skip the next [count] points of the geometry. Less points are skipped if there are less than
     * [count] points left in the iterator.
     *
     * @param count the number of points to skip.
     */
    fun skip(count: Int) {
        var skipped = 0
        while (skipped++ <= count && hasNext()) { next() }
    }
}
