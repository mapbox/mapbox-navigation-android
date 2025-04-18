package com.mapbox.navigation.base.utils

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
