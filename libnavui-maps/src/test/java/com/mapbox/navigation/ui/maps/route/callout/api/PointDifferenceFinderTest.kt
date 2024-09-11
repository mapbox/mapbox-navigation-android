package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.geojson.Point
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PointDifferenceFinderTest {

    private val mainRoutePoints = listOf(
        Point.fromLngLat(133.0, 33.0),
        Point.fromLngLat(134.0, 34.0),
        Point.fromLngLat(135.0, 35.0),
        Point.fromLngLat(136.0, 36.0),
        Point.fromLngLat(137.0, 37.0),
    )

    @Test
    fun `alternative deviation is in the middle of the route`() {
        val altRoutePoints = listOf(
            Point.fromLngLat(133.0, 33.0),
            Point.fromLngLat(134.0, 34.0),
            Point.fromLngLat(120.0, 60.0),
            Point.fromLngLat(137.0, 37.0),
        )

        val expectedResult = listOf(
            Point.fromLngLat(120.0, 60.0),
        )
        val actualResult = PointDifferenceFinder.extractDifference(
            mainRoutePoints,
            altRoutePoints,
            firstIndexOfAlt = null,
        )
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `ignore firstIndexOfAlt if it's out of bounds`() {
        val altRoutePoints = listOf(
            Point.fromLngLat(133.0, 33.0),
            Point.fromLngLat(134.0, 34.0),
            Point.fromLngLat(120.0, 60.0),
            Point.fromLngLat(137.0, 37.0),
        )

        val expectedResult = listOf(
            Point.fromLngLat(120.0, 60.0),
        )
        val actualResult = PointDifferenceFinder.extractDifference(
            mainRoutePoints,
            altRoutePoints,
            firstIndexOfAlt = 5,
        )
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `firstIndexOfAlt should define the start bound`() {
        val altRoutePoints = listOf(
            Point.fromLngLat(118.0, 58.0),
            Point.fromLngLat(119.0, 59.0),
            Point.fromLngLat(120.0, 60.0),
            Point.fromLngLat(137.0, 37.0),
        )

        val expectedResult = listOf(
            Point.fromLngLat(119.0, 59.0),
            Point.fromLngLat(120.0, 60.0),
        )
        val actualResult = PointDifferenceFinder.extractDifference(
            mainRoutePoints,
            altRoutePoints,
            firstIndexOfAlt = 1,
        )
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `all of alternative points are different`() {
        val altRoutePoints = listOf(
            Point.fromLngLat(120.0, 60.0),
            Point.fromLngLat(121.0, 61.0),
            Point.fromLngLat(122.0, 62.0),
        )
        val actualResult = PointDifferenceFinder.extractDifference(
            mainRoutePoints,
            altRoutePoints,
            firstIndexOfAlt = null,
        )

        assertEquals(altRoutePoints, actualResult)
    }

    @Test
    fun `alternative has the same points as the main route`() {
        val altRoutePoints = mainRoutePoints
        val actualResult = PointDifferenceFinder.extractDifference(
            mainRoutePoints,
            altRoutePoints,
            firstIndexOfAlt = null,
        )

        assertTrue(actualResult.isEmpty())
    }
}
