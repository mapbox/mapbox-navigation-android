package com.mapbox.navigation.ui.maps.camera.internal

import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Size
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class NavigationCameraExTest {

    @Test
    fun `single pixel padding`() {
        val testCases = listOf(
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(50.0, 50.0, 50.0, 50.0),
                expectedResult = true,
            ),
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(33.3, 33.33334, 66.6667, 66.6667),
                expectedResult = true,
            ),
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(40.0, 50.0, 50.0, 50.0),
                expectedResult = false,
            ),
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(50.0, 50.0, 40.0, 50.0),
                expectedResult = false,
            ),
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(50.0, 0.0, 40.0, 50.0),
                expectedResult = false,
            ),
            SinglePixelPaddingTestCase(
                // width, height
                mapSize = Size(100f, 100f),
                // top, left, bottom, right
                mapPadding = EdgeInsets(50.0, 0.0, 40.0, 0.0),
                expectedResult = false,
            ),
        )

        testCases.forEach { testCase ->
            val mapboxMap = mockk<MapboxMap> {
                every { getSize() } returns testCase.mapSize
                every { cameraState.padding } returns testCase.mapPadding
            }

            val result = mapboxMap.isSinglePixelPadding()
            val msg = "failed for $testCase"
            if (testCase.expectedResult) {
                Assert.assertTrue(msg, result)
            } else {
                Assert.assertFalse(msg, result)
            }
        }
    }
}

private data class SinglePixelPaddingTestCase(
    val mapSize: Size,
    val mapPadding: EdgeInsets,
    val expectedResult: Boolean,
)
