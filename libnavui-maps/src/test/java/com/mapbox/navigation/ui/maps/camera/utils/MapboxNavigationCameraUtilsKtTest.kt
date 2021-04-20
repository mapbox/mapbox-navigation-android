package com.mapbox.navigation.ui.maps.camera.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxNavigationCameraUtilsKtTest {

    @Test
    fun `test normalizeBearing 1`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = 10.0,
            targetBearing = 0.0
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 2`() {
        val expected = 10.0

        val actual = normalizeBearing(
            currentBearing = 0.0,
            targetBearing = 10.0
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 3`() {
        val expected = -0.5

        val actual = normalizeBearing(
            currentBearing = 1.0,
            targetBearing = 359.5
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 4`() {
        val expected = 360.0

        val actual = normalizeBearing(
            currentBearing = 359.5,
            targetBearing = 0.0
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 5`() {
        val expected = 361.0

        val actual = normalizeBearing(
            currentBearing = 359.5,
            targetBearing = 1.0
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test normalizeBearing 6`() {
        val expected = 110.0

        val actual = normalizeBearing(
            currentBearing = 50.0,
            targetBearing = 110.0
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 7`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = -0.0,
            targetBearing = 360.0
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 8`() {
        val expected = -0.0

        val actual = normalizeBearing(
            currentBearing = -0.0,
            targetBearing = 0.0
        )

        assertEquals(expected, actual, 0.000001)
    }

    @Test
    fun `test normalizeBearing 9`() {
        val expected = 0.0

        val actual = normalizeBearing(
            currentBearing = 27.254667247679752,
            targetBearing = 0.0
        )

        assertEquals(expected, actual, 1E-14)
    }
}
