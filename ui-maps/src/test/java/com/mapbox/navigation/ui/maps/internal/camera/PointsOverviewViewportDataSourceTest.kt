package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PointsOverviewViewportDataSourceTest {

    private val mapboxMap: MapboxMap = mockk(relaxed = true)

    private val emptyCameraState = CameraState(
        NULL_ISLAND_POINT,
        EMPTY_EDGE_INSETS,
        0.0,
        BEARING_NORTH,
        ZERO_PITCH,
    )

    private val viewportDataSource = PointsOverviewViewportDataSource(mapboxMap)

    @Before
    fun setUp() {
        every { mapboxMap.cameraState } returns emptyCameraState
    }

    @Test
    fun `empty source initializes at null island`() {
        val data = viewportDataSource.cameraOptions

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(viewportDataSource.options.maxZoom)
                padding(EMPTY_EDGE_INSETS)
            },
            data,
        )
    }

    @Test
    fun `frames only the configured points`() {
        val points = listOf(
            Point.fromLngLat(-30.0, -40.0),
            Point.fromLngLat(-31.0, -41.0),
        )
        val overviewZoom = viewportDataSource.options.maxZoom
        val overviewCameraOptions = createCameraOptions {
            center(points.first())
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                points,
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        viewportDataSource.additionalPointsToFrame(points)
        viewportDataSource.evaluate()

        assertEquals(overviewCameraOptions, viewportDataSource.cameraOptions)
    }

    @Test
    fun `no points falls back to the current camera state`() {
        val currentCameraState = CameraState(
            Point.fromLngLat(32.4, 56.7),
            EdgeInsets(1.0, 1.0, 2.0, 2.0),
            15.0,
            33.0,
            55.0,
        )
        every { mapboxMap.cameraState } returns currentCameraState

        viewportDataSource.evaluate()

        assertEquals(currentCameraState.center, viewportDataSource.cameraOptions.center)
    }

    @Test
    fun `bearing and pitch overrides are applied`() {
        val points = listOf(Point.fromLngLat(-30.0, -40.0))
        every {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        } returns createCameraOptions {
            center(points.first())
        }

        viewportDataSource.additionalPointsToFrame(points)
        viewportDataSource.bearingPropertyOverride(42.0)
        viewportDataSource.pitchPropertyOverride(12.0)
        viewportDataSource.evaluate()

        val data = viewportDataSource.cameraOptions
        assertEquals(42.0, data.bearing!!, 0.0)
        assertEquals(12.0, data.pitch!!, 0.0)
    }

    @Test
    fun `inactive source does not update viewport data`() {
        val before = viewportDataSource.cameraOptions

        viewportDataSource.setActive(false)
        viewportDataSource.additionalPointsToFrame(
            listOf(Point.fromLngLat(-30.0, -40.0)),
        )
        viewportDataSource.evaluate()

        assertEquals(before, viewportDataSource.cameraOptions)
    }

    private fun createCameraOptions(block: CameraOptions.Builder.() -> Unit): CameraOptions {
        return CameraOptions.Builder()
            .zoom(emptyCameraState.zoom)
            .bearing(emptyCameraState.bearing)
            .padding(emptyCameraState.padding)
            .center(emptyCameraState.center)
            .pitch(emptyCameraState.pitch)
            .apply(block)
            .build()
    }
}
