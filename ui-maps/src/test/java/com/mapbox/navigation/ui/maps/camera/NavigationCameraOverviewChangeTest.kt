package com.mapbox.navigation.ui.maps.camera

import android.os.SystemClock
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.OverviewChangeObserver
import com.mapbox.navigation.ui.maps.camera.data.ViewportData
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceUpdateObserver
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.AnimatorsCreator
import com.mapbox.navigation.ui.maps.camera.transition.FullAnimatorSet
import com.mapbox.navigation.ui.maps.camera.transition.MapboxAnimatorSet
import com.mapbox.navigation.ui.maps.camera.transition.MapboxAnimatorSetEndListener
import com.mapbox.navigation.ui.maps.camera.transition.MapboxAnimatorSetListener
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
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

/**
 * Covers the replay of the route overview transition when the overview frame jumps by a large
 * amount, see [OverviewChangeObserver].
 */
class NavigationCameraOverviewChangeTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxMap: MapboxMap = mockk(relaxUnitFun = true) {
        every { cameraState } returns mockk(relaxed = true)
    }
    private val cameraPlugin: CameraAnimationsPlugin = mockk(relaxUnitFun = true)

    private val transitionListenerSlot = slot<MapboxAnimatorSetListener>()
    private val transitionBlock: FullAnimatorSet.() -> Unit = {
        every { addListener(capture(transitionListenerSlot)) } just Runs
        every { makeInstant() } answers {}
    }
    private val frameBlock: MapboxAnimatorSet.() -> Unit = {
        every { addAnimationEndListener(any<MapboxAnimatorSetEndListener>()) } just Runs
        every { makeInstant() } answers {}
    }
    private val followingAnimatorSet: FullAnimatorSet = mockk(
        relaxUnitFun = true,
        block = transitionBlock,
    )
    private val overviewAnimatorSet: FullAnimatorSet = mockk(
        relaxUnitFun = true,
        block = transitionBlock,
    )
    private val frameAnimatorSet: MapboxAnimatorSet = mockk(
        relaxUnitFun = true,
        block = frameBlock,
    )
    private val followingCameraOptions = CameraOptions.Builder().zoom(1.0).build()
    private val overviewCameraOptions = CameraOptions.Builder().zoom(2.0).build()
    private val animatorsCreator: AnimatorsCreator = mockk(relaxUnitFun = true) {
        every { transitionToFollowing(any(), any()) } returns followingAnimatorSet
        every { transitionToRouteOverview(any(), any()) } returns overviewAnimatorSet
        every { updateFrameForFollowing(any(), any()) } returns frameAnimatorSet
        every { updateFrameForOverview(any(), any()) } returns frameAnimatorSet
    }

    private val overviewChangeObserverSlot = slot<OverviewChangeObserver>()
    private val viewportDataSource: MapboxNavigationViewportDataSource = mockk(
        relaxUnitFun = true,
    ) {
        every { getViewportData() } returns ViewportData(
            followingCameraOptions,
            overviewCameraOptions,
            CameraOptions.Builder().build(),
        )
        every { registerUpdateObserver(any<ViewportDataSourceUpdateObserver>()) } just Runs
        every {
            registerOverviewChangeObserver(capture(overviewChangeObserverSlot))
        } just Runs
    }

    private val customStateTransitionOptions =
        NavigationCameraTransitionOptions.Builder().maxDuration(1234L).build()

    private lateinit var navigationCamera: NavigationCamera

    @Before
    fun setUp() {
        mockkStatic(SystemClock::elapsedRealtime)
        every { SystemClock.elapsedRealtime() } returns 10_000L
        every { mapboxMap.isValid() } returns true
        navigationCamera = NavigationCamera(mapboxMap, cameraPlugin, viewportDataSource, animatorsCreator)
    }

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::elapsedRealtime)
    }

    @Test
    fun `overview change callback in overview replays the overview transition`() {
        enterOverview()

        notifyOverviewChange()

        verifyOverviewTransitions(count = 2)
    }

    @Test
    fun `replayed overview transition uses the state options of the latest overview request`() {
        enterOverview()

        notifyOverviewChange()

        verify(exactly = 2) {
            animatorsCreator.transitionToRouteOverview(
                overviewCameraOptions,
                customStateTransitionOptions,
            )
        }
    }

    @Test
    fun `overview change callback while following does not request the overview`() {
        enterFollowing()

        notifyOverviewChange()

        verifyOverviewTransitions(count = 0)
    }

    @Test
    fun `overview change callback while idle does not request the overview`() {
        notifyOverviewChange()

        verifyOverviewTransitions(count = 0)
    }

    @Test
    fun `overview change callback while transitioning to overview restarts the transition`() {
        navigationCamera.requestNavigationCameraToOverview(customStateTransitionOptions)
        transitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)

        notifyOverviewChange()

        verifyOverviewTransitions(count = 2)
    }

    @Test
    fun `overview change callback with the map invalid does not replay the transition`() {
        enterOverview()
        every { mapboxMap.isValid() } returns false

        notifyOverviewChange()

        verifyOverviewTransitions(count = 1)
    }

    @Test
    fun `replayed transition leaves the camera in overview`() {
        enterOverview()

        notifyOverviewChange()
        transitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        transitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
    }

    @Test
    fun `detaching stops listening to the viewport data source`() {
        navigationCamera.onDestroy()

        verify(exactly = 1) {
            viewportDataSource.unregisterUpdateObserver(any<ViewportDataSourceUpdateObserver>())
        }
        verify(exactly = 1) {
            viewportDataSource.unregisterOverviewChangeObserver(
                overviewChangeObserverSlot.captured,
            )
        }
    }

    private fun enterOverview() {
        navigationCamera.requestNavigationCameraToOverview(customStateTransitionOptions)
        transitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        transitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)
    }

    private fun enterFollowing() {
        navigationCamera.requestNavigationCameraToFollowing()
        transitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        transitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)
    }

    private fun notifyOverviewChange() {
        overviewChangeObserverSlot.captured.onOverviewChange()
    }

    private fun verifyOverviewTransitions(count: Int) {
        verify(exactly = count) {
            animatorsCreator.transitionToRouteOverview(any(), any())
        }
    }
}
