package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Size
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the frame the camera transitions to when it is asked to follow before the viewport data
 * source has evaluated.
 *
 * [MapboxNavigationViewportDataSource.evaluate] waits for the map to report its size. Until it
 * runs, the source holds only the frame it seeded at construction: default [FollowingFrameOptions]
 * and un-overridden property fallbacks. A map that has been created and shown but not laid out
 * sits in exactly that window.
 *
 * `NavigationCamera.getFollowingTransition` takes its target from whatever
 * [MapboxNavigationViewportDataSource.getViewportData] offers at that moment. A seed frame there
 * snaps the camera to north-up, pitch 45, zoom 16.35, at null island.
 */
class MapboxNavigationViewportDataSourceInitialFrameTest {

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

    /** Callbacks handed to `MapboxMap.whenSizeReady`, invoked only by [reportMapSizeReady]. */
    private val pendingSizeReadyCallbacks = mutableListOf<() -> Unit>()

    private lateinit var dataSource: MapboxNavigationViewportDataSource

    @Before
    fun setUp() {
        every { mapboxMap.cameraState } returns emptyCameraState
        every { mapboxMap.getSize() } returns Size(1000f, 1000f)
        val actionSlot = slot<() -> Unit>()
        every { mapboxMap.whenSizeReady(capture(actionSlot)) } answers {
            pendingSizeReadyCallbacks += actionSlot.captured
            mockk<Cancelable>(relaxed = true)
        }

        dataSource = MapboxNavigationViewportDataSource(mapboxMap)
    }

    private fun reportMapSizeReady() {
        val callbacks = pendingSizeReadyCallbacks.toList()
        pendingSizeReadyCallbacks.clear()
        callbacks.forEach { it() }
    }

    private fun setInitialCameraState() {
        dataSource.followingBearingPropertyOverride(INITIAL_BEARING)
        dataSource.followingPitchPropertyOverride(INITIAL_PITCH)
        dataSource.followingZoomPropertyOverride(INITIAL_ZOOM)
    }

    /** The frame must carry the initial camera state even though no evaluation has run yet. */
    @Test
    fun followingOverridesAreReflectedInTheFrameProducedBeforeTheMapSizeIsReady() {
        setInitialCameraState()
        dataSource.evaluate()

        val frame = dataSource.getViewportData().cameraForFollowing

        assertEquals(
            "bearing must come from the override, not the BEARING_NORTH fallback",
            INITIAL_BEARING,
            frame.bearing!!,
            PRECISION,
        )
        assertEquals(
            "pitch must come from the override, not defaultPitch " +
                "(${dataSource.options.followingFrameOptions.defaultPitch})",
            INITIAL_PITCH,
            frame.pitch!!,
            PRECISION,
        )
        assertEquals(
            "zoom must come from the override, not maxZoom " +
                "(${dataSource.options.followingFrameOptions.maxZoom})",
            INITIAL_ZOOM,
            frame.zoom!!,
            PRECISION,
        )
    }

    /**
     * The companion case: once the map reports its size an evaluation runs, and the same overrides
     * reach the frame through the normal path.
     */
    @Test
    fun followingOverridesAreReflectedInTheFrameOnceTheMapSizeIsReady() {
        setInitialCameraState()
        dataSource.evaluate()
        reportMapSizeReady()

        val frame = dataSource.getViewportData().cameraForFollowing

        assertEquals(INITIAL_BEARING, frame.bearing!!, PRECISION)
        assertEquals(INITIAL_PITCH, frame.pitch!!, PRECISION)
        assertEquals(INITIAL_ZOOM, frame.zoom!!, PRECISION)
    }

    private companion object {
        const val INITIAL_BEARING = 180.0
        const val INITIAL_PITCH = 55.0
        const val INITIAL_ZOOM = 15.0
        const val PRECISION = 0.000001
    }
}
