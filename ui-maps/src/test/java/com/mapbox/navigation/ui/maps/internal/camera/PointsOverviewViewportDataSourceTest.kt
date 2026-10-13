package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PointsOverviewViewportDataSourceTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

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
    fun `empty source has no frame to move the camera to`() {
        assertEquals(CameraOptions.Builder().build(), viewportDataSource.cameraOptions)
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
    fun `points the map cannot frame leave the frame empty`() {
        every {
            mapboxMap.cameraForCoordinates(any<List<Point>>(), any<CameraOptions>(), null, null, null)
        } returns CameraOptions.Builder().build()

        viewportDataSource.additionalPointsToFrame(listOf(Point.fromLngLat(-30.0, -40.0)))
        viewportDataSource.evaluate()

        assertEquals(CameraOptions.Builder().build(), viewportDataSource.cameraOptions)
    }

    @Test
    fun `overrides are used when there are no points to frame`() {
        val overrideCenter = Point.fromLngLat(11.0, 12.0)
        viewportDataSource.centerPropertyOverride(overrideCenter)
        viewportDataSource.zoomPropertyOverride(12.0)

        viewportDataSource.evaluate()

        assertEquals(
            CameraOptions.Builder().center(overrideCenter).zoom(12.0).build(),
            viewportDataSource.cameraOptions,
        )
    }

    @Test
    fun `without points bearing and pitch overrides stay out when their updates are not allowed`() {
        val overrideCenter = Point.fromLngLat(11.0, 12.0)
        viewportDataSource.centerPropertyOverride(overrideCenter)
        viewportDataSource.zoomPropertyOverride(12.0)
        viewportDataSource.bearingPropertyOverride(90.0)
        viewportDataSource.pitchPropertyOverride(30.0)
        viewportDataSource.options.apply {
            bearingUpdatesAllowed = false
            pitchUpdatesAllowed = false
        }

        viewportDataSource.evaluate()

        assertEquals(
            CameraOptions.Builder().center(overrideCenter).zoom(12.0).build(),
            viewportDataSource.cameraOptions,
        )
    }

    @Test
    fun `no points leaves the frame empty`() {
        viewportDataSource.evaluate()

        assertEquals(CameraOptions.Builder().build(), viewportDataSource.cameraOptions)
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
