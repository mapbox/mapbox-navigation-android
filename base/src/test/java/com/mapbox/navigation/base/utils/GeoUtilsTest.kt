package com.mapbox.navigation.base.utils

import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class GeoUtilsTest {

    @Before
    fun setUp() {
        mockkObject(CoreGeoUtilsWrapper)
        every { CoreGeoUtilsWrapper.getTopoLinkId(any(), any(), any()) } returns TEST_ID
    }

    @After
    fun tearDown() {
        unmockkObject(CoreGeoUtilsWrapper)
    }

    @Test
    fun testWrapsPointsWithLineString() {
        val id = GeoUtils.getTopoLinkId(TEST_POINTS, 1, 2)
        assertEquals(TEST_ID, id)
        verify {
            CoreGeoUtilsWrapper.getTopoLinkId(
                eq(LineString.fromLngLats(TEST_POINTS)),
                eq(1),
                eq(2),
            )
        }
    }

    @Test
    fun testDefaultStartEndIndex() {
        val id = GeoUtils.getTopoLinkId(TEST_POINTS)
        assertEquals(TEST_ID, id)
        verify {
            CoreGeoUtilsWrapper.getTopoLinkId(
                eq(LineString.fromLngLats(TEST_POINTS)),
                eq(0),
                eq(TEST_POINTS.size),
            )
        }
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testThrowsExceptionWhenStartIndexLessThanZero() {
        GeoUtils.getTopoLinkId(TEST_POINTS, -1, 1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testThrowsExceptionWhenStartIndexGreaterThanGeometrySize() {
        GeoUtils.getTopoLinkId(TEST_POINTS, TEST_POINTS.size + 1, 0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testThrowsExceptionWhenEndIndexIndexLessThanZero() {
        GeoUtils.getTopoLinkId(TEST_POINTS, 0, -1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testThrowsExceptionWhenEndIndexGreaterThanGeometrySize() {
        GeoUtils.getTopoLinkId(TEST_POINTS, 0, TEST_POINTS.size + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testThrowsExceptionWhenStartIndexGreaterThanEndIndex() {
        GeoUtils.getTopoLinkId(TEST_POINTS, 1, 0)
    }

    @Test
    fun testForwardsLineStringToCore() {
        val geometry = LineString.fromLngLats(TEST_POINTS)
        val id = GeoUtils.getTopoLinkId(geometry, 0, TEST_POINTS.size)
        assertEquals(TEST_ID, id)
        verify {
            CoreGeoUtilsWrapper.getTopoLinkId(
                eq(geometry),
                eq(0),
                eq(TEST_POINTS.size),
            )
        }
    }

    @Test
    fun testForwardsMultiPointToCore() {
        val geometry = MultiPoint.fromLngLats(TEST_POINTS)
        val id = GeoUtils.getTopoLinkId(geometry, 0, TEST_POINTS.size)
        assertEquals(TEST_ID, id)
        verify {
            CoreGeoUtilsWrapper.getTopoLinkId(
                eq(geometry),
                eq(0),
                eq(TEST_POINTS.size),
            )
        }
    }

    companion object {
        const val TEST_ID = 123L

        val TEST_POINTS = listOf(
            Point.fromLngLat(10.0, 20.0),
            Point.fromLngLat(20.0, 30.0),
            Point.fromLngLat(30.0, 40.0),
        )
    }
}
