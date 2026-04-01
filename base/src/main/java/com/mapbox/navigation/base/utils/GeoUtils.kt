package com.mapbox.navigation.base.utils

import androidx.annotation.WorkerThread
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.CoordinateContainer
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

private typealias CoreGeoUtils = com.mapbox.navigator.GeoUtils

// Just for being able to mock CoreGeoUtils
internal object CoreGeoUtilsWrapper {
    fun getTopoLinkId(geometry: Geometry, startIndex: Int, endIndex: Int): Long {
        return CoreGeoUtils.getTopoLinkId(geometry, startIndex, endIndex)
    }

    fun getWayId(edgeId: Long): Expected<String, Long> {
        return CoreGeoUtils.getWayId(edgeId)
    }

    fun getWayId(shape: Geometry, startIndex: Int, endIndex: Int): Expected<String, List<Long>> {
        return CoreGeoUtils.getWayId(shape, startIndex, endIndex)
    }
}

/**
 * Utility class for geographic-related functions.
 */
@ExperimentalPreviewMapboxNavigationAPI
object GeoUtils {

    /**
     * Provides the unique identifier for the given link. Link is a part of a road
     * between two intersection. The identifier is unique within the current road graph
     * within a reasonable some area. It may be used to identify the link in the road graph.
     *
     * The identifier is stable and does not change between different versions of the road network.
     * The identifier is not guaranteed to be unique between different versions of a map,
     * if link geometry changes. It is not guaranteed to be unique between different regions
     * of the same map.
     *
     * @param geometry [LineString] object representing link geometry
     * @param startIndex start index (inclusive) of the link
     * @param endIndex end index (exclusive) of the link
     *
     * @throws IndexOutOfBoundsException if [startIndex] or [endIndex] are outside the bounds of [geometry]
     * @throws IllegalArgumentException if [startIndex] > [endIndex]
     */
    fun getTopoLinkId(geometry: LineString, startIndex: Int, endIndex: Int): Long {
        checkIndices(geometry, startIndex, endIndex)
        return CoreGeoUtilsWrapper.getTopoLinkId(geometry, startIndex, endIndex)
    }

    /**
     * Provides the unique identifier for the given link. Link is a part of a road
     * between two intersection. The identifier is unique within the current road graph
     * within a reasonable some area. It may be used to identify the link in the road graph.
     *
     * The identifier is stable and does not change between different versions of the road network.
     * The identifier is not guaranteed to be unique between different versions of a map,
     * if link geometry changes. It is not guaranteed to be unique between different regions
     * of the same map.
     *
     * @param geometry [MultiPoint] object representing link geometry
     * @param startIndex start index (inclusive) of the link
     * @param endIndex end index (exclusive) of the link
     *
     * @throws IndexOutOfBoundsException if [startIndex] or [endIndex] are outside the bounds of [geometry]
     * @throws IllegalArgumentException if [startIndex] > [endIndex]
     */
    fun getTopoLinkId(geometry: MultiPoint, startIndex: Int, endIndex: Int): Long {
        checkIndices(geometry, startIndex, endIndex)
        return CoreGeoUtilsWrapper.getTopoLinkId(geometry, startIndex, endIndex)
    }

    /**
     * Provides the unique identifier for the given link. Link is a part of a road
     * between two intersection. The identifier is unique within the current road graph
     * within a reasonable some area. It may be used to identify the link in the road graph.
     *
     * The identifier is stable and does not change between different versions of the road network.
     * The identifier is not guaranteed to be unique between different versions of a map,
     * if link geometry changes. It is not guaranteed to be unique between different regions
     * of the same map.
     *
     * @param points a list of [Point] representing link geometry
     * @param startIndex start index (inclusive) of the link. Defaults to 0
     * @param endIndex end index (exclusive) of the link. Defaults to the size of the [points] list
     *
     * @throws IndexOutOfBoundsException if [startIndex] or [endIndex] are outside the bounds of [points]
     * @throws IllegalArgumentException if [startIndex] > [endIndex]
     */
    @JvmOverloads
    fun getTopoLinkId(points: List<Point>, startIndex: Int = 0, endIndex: Int = points.size): Long {
        return getTopoLinkId(LineString.fromLngLats(points), startIndex, endIndex)
    }

    /**
     * Provides the OSM way id for the given directed edge.
     *
     * May perform blocking access to the on-device routing graph / tiles.
     *
     * @param edgeId Directed edge id
     * @return OSM way id on success (including 0 when that is the real way id). On failure, the
     * error string describes the reason (e.g. no graph reader, invalid id, missing tile).
     */
    @WorkerThread
    fun getWayId(edgeId: Long): Expected<String, Long> {
        return CoreGeoUtilsWrapper.getWayId(edgeId)
    }

    /**
     * Provides the OSM way ids encountered along a polyline span, map-matched to the routing graph.
     *
     * Returns an ordered list of distinct consecutive way ids along the matched path. On any failure
     * the result is an error (e.g. no graph reader, invalid span, search failure, missing tile).
     *
     * May perform blocking graph search and tile IO.
     *
     * @param geometry [LineString] object representing the polyline span
     * @param startIndex start index of the span (inclusive)
     * @param endIndex end index of the span (exclusive); span must contain at least two points
     *
     * @throws IndexOutOfBoundsException if [startIndex] or [endIndex] are outside the bounds of [geometry]
     * @throws IllegalArgumentException if [startIndex] > [endIndex]
     */
    @WorkerThread
    fun getWayId(
        geometry: LineString,
        startIndex: Int,
        endIndex: Int,
    ): Expected<String, List<Long>> {
        checkIndices(geometry, startIndex, endIndex)
        return CoreGeoUtilsWrapper.getWayId(geometry, startIndex, endIndex)
    }

    /**
     * Provides the OSM way ids encountered along a polyline span, map-matched to the routing graph.
     *
     * Returns an ordered list of distinct consecutive way ids along the matched path. On any failure
     * the result is an error (e.g. no graph reader, invalid span, search failure, missing tile).
     *
     * May perform blocking graph search and tile IO.
     *
     * @param points a list of [Point] representing the polyline span
     * @param startIndex start index of the span (inclusive). Defaults to 0
     * @param endIndex end index of the span (exclusive). Defaults to the size of the [points] list
     *
     * @throws IndexOutOfBoundsException if [startIndex] or [endIndex] are outside the bounds of [points]
     * @throws IllegalArgumentException if [startIndex] > [endIndex]
     */
    @WorkerThread
    @JvmOverloads
    fun getWayId(
        points: List<Point>,
        startIndex: Int = 0,
        endIndex: Int = points.size,
    ): Expected<String, List<Long>> {
        return getWayId(LineString.fromLngLats(points), startIndex, endIndex)
    }

    private fun checkIndices(
        coordinates: CoordinateContainer<MutableList<Point>>,
        startIndex: Int,
        endIndex: Int,
    ) {
        val indices = 0..coordinates.coordinates().size
        if (startIndex !in indices) {
            throw IndexOutOfBoundsException("startIndex = $startIndex")
        } else if (endIndex !in indices) {
            throw IndexOutOfBoundsException("endIndex = $endIndex")
        } else if (startIndex > endIndex) {
            throw IllegalArgumentException("startIndex($startIndex) > endIndex($endIndex)")
        }
    }
}
