package com.mapbox.navigation.ui.maps.camera

import android.animation.ValueAnimator
import android.os.Looper
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.ViewportData
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceUpdateObserver
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.utils.getAnimatorsFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * A viewport data source that has no data yet hands out empty frames. Every transition and frame
 * update the camera derives from them must be a no-op that still completes.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationCameraEmptyFrameTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxMap: MapboxMap = mockk(relaxed = true)
    private val cameraPlugin: CameraAnimationsPlugin = mockk(relaxed = true)

    private val emptyFrame = CameraOptions.Builder().build()
    private val observerSlot = slot<ViewportDataSourceUpdateObserver>()
    private val viewportDataSource: ViewportDataSource = mockk(relaxUnitFun = true) {
        every { getViewportData() } returns ViewportData(emptyFrame, emptyFrame, emptyFrame)
        every { registerUpdateObserver(capture(observerSlot)) } just Runs
    }

    private lateinit var navigationCamera: NavigationCamera

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
        every { cameraPlugin.getAnimatorsFactory() } returns mockk(relaxed = true)
        every { mapboxMap.isValid() } returns true
        navigationCamera = NavigationCamera(mapboxMap, cameraPlugin, viewportDataSource)
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.camera.utils.MapboxNavigationCameraUtilsKt")
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun verifyNoCameraAnimatorsCreated() {
        verify(exactly = 0) {
            cameraPlugin.createCenterAnimator(any(), any(), any<(ValueAnimator.() -> Unit)>())
            cameraPlugin.createZoomAnimator(any(), any())
            cameraPlugin.createBearingAnimator(any(), any(), any())
            cameraPlugin.createPitchAnimator(any(), any())
            cameraPlugin.createPaddingAnimator(any(), any())
        }
    }

    @Test
    fun `following with an empty frame completes without animating`() {
        navigationCamera.requestNavigationCameraToFollowing()
        idle()

        assertEquals(NavigationCameraState.FOLLOWING, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }

    @Test
    fun `frame update while following with an empty frame does not animate`() {
        navigationCamera.requestNavigationCameraToFollowing()
        idle()

        observerSlot.captured.viewportDataSourceUpdated(
            ViewportData(emptyFrame, emptyFrame, emptyFrame),
        )
        idle()

        assertEquals(NavigationCameraState.FOLLOWING, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }

    @Test
    fun `route overview with an empty frame completes without animating`() {
        navigationCamera.requestNavigationCameraToOverview()
        idle()

        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }

    @Test
    fun `frame update while in overview with an empty frame does not animate`() {
        navigationCamera.requestNavigationCameraToOverview()
        idle()

        observerSlot.captured.viewportDataSourceUpdated(
            ViewportData(emptyFrame, emptyFrame, emptyFrame),
        )
        idle()

        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }

    @Test
    fun `points overview with an empty frame completes without animating`() {
        navigationCamera.requestNavigationCameraToPointsOverview()
        idle()

        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }

    @Test
    fun `frame update while in points overview with an empty frame does not animate`() {
        navigationCamera.requestNavigationCameraToPointsOverview()
        idle()

        observerSlot.captured.viewportDataSourceUpdated(
            ViewportData(emptyFrame, emptyFrame, emptyFrame),
        )
        idle()

        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
        verifyNoCameraAnimatorsCreated()
    }
}
