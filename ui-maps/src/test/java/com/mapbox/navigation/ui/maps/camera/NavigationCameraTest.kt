package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_FRAME_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_STATE_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.data.ViewportData
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceUpdateObserver
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransition
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.camera.transition.TransitionEndListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigationCameraTest {

    private val mapboxMap: MapboxMap = mockk(relaxUnitFun = true)
    private val cameraPlugin: CameraAnimationsPlugin = mockk(relaxUnitFun = true)

    private val internalTransitionListenerSlot = slot<Animator.AnimatorListener>()
    private val internalFrameListenerSlot = slot<Animator.AnimatorListener>()
    private val transitionBlock: AnimatorSet.() -> Unit = {
        every { childAnimations } returns arrayListOf<Animator>(
            mockk<ValueAnimator>(),
            mockk<ValueAnimator>(),
            mockk<ValueAnimator>(),
        )
        every { addListener(capture(internalTransitionListenerSlot)) } just Runs
        every { setDuration(any()) } returns this
    }
    private val frameBlock: AnimatorSet.() -> Unit = {
        every { childAnimations } returns arrayListOf<Animator>(
            mockk<ValueAnimator>(),
            mockk<ValueAnimator>(),
            mockk<ValueAnimator>(),
        )
        every { addListener(capture(internalFrameListenerSlot)) } just Runs
        every { setDuration(any()) } returns this
    }
    private val followingAnimatorSet: AnimatorSet = mockk(
        relaxUnitFun = true,
        block = transitionBlock,
    )
    private val overviewAnimatorSet: AnimatorSet = mockk(
        relaxUnitFun = true,
        block = transitionBlock,
    )
    private val followingFrameAnimatorSet: AnimatorSet = mockk(
        relaxUnitFun = true,
        block = frameBlock,
    )
    private val overviewFrameAnimatorSet: AnimatorSet = mockk(
        relaxUnitFun = true,
        block = frameBlock,
    )
    private val stateTransition: NavigationCameraStateTransition = mockk(relaxUnitFun = true) {
        every { transitionToFollowing(any(), any()) } returns followingAnimatorSet
        every { transitionToOverview(any(), any()) } returns overviewAnimatorSet
        every { updateFrameForFollowing(any(), any()) } returns followingFrameAnimatorSet
        every { updateFrameForOverview(any(), any()) } returns overviewFrameAnimatorSet
    }

    private val internalDataSourceObserverSlot = slot<ViewportDataSourceUpdateObserver>()
    private val followingCameraOptions: CameraOptions = mockk()
    private val overviewCameraOptions: CameraOptions = mockk()
    private val viewportData: ViewportData = mockk {
        every { cameraForFollowing } returns followingCameraOptions
        every { cameraForOverview } returns overviewCameraOptions
    }
    private val viewportDataSource: ViewportDataSource = mockk(relaxUnitFun = true) {
        every { getViewportData() } returns viewportData
        every { registerUpdateObserver(capture(internalDataSourceObserverSlot)) } just Runs
    }

    private val transitionEndListener = mockk<TransitionEndListener>(relaxUnitFun = true)

    private lateinit var navigationCamera: NavigationCamera

    @Before
    fun setup() {
        navigationCamera = NavigationCamera(
            mapboxMap,
            cameraPlugin,
            viewportDataSource,
            stateTransition,
        )
    }

    @Test
    fun `defaults to IDLE state`() {
        assertEquals(NavigationCameraState.IDLE, navigationCamera.state)
    }

    @Test
    fun `init registers data source listener`() {
        verify(exactly = 1) { viewportDataSource.registerUpdateObserver(any()) }
    }

    @Test
    fun `when following requested, transition executed`() {
        navigationCamera.requestNavigationCameraToFollowing()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToFollowing,
            followingCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            followingAnimatorSet,
        )
    }

    @Test
    fun `when overview requested, transition executed`() {
        navigationCamera.requestNavigationCameraToOverview()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToOverview,
            overviewCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            overviewAnimatorSet,
        )
    }

    /**
     * workaround for https://github.com/mapbox/mapbox-maps-android/issues/277
     */
    @Test
    fun `when following requested, anchor nullified`() {
        navigationCamera.requestNavigationCameraToFollowing()

        verify { cameraPlugin.anchor = null }
    }

    /**
     * workaround for https://github.com/mapbox/mapbox-maps-android/issues/277
     */
    @Test
    fun `when overview requested, anchor nullified`() {
        navigationCamera.requestNavigationCameraToOverview()

        verify { cameraPlugin.anchor = null }
    }

    @Test
    fun `when following requested twice, transition executed once`() {
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        navigationCamera.requestNavigationCameraToFollowing()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToFollowing,
            followingCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            followingAnimatorSet,
            times = 1,
        )
    }

    @Test
    fun `when overview requested twice, transition executed once`() {
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        navigationCamera.requestNavigationCameraToOverview()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToOverview,
            overviewCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            overviewAnimatorSet,
            times = 1,
        )
    }

    @Test
    fun `when following requested, state changes`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        assertEquals(NavigationCameraState.TRANSITION_TO_FOLLOWING, navigationCamera.state)

        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)
        assertEquals(NavigationCameraState.FOLLOWING, navigationCamera.state)
    }

    @Test
    fun `when overview requested, state changes`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        assertEquals(NavigationCameraState.TRANSITION_TO_OVERVIEW, navigationCamera.state)

        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)
        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
    }

    @Test
    fun `when following request canceled, state does not changes to IDLE`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)
        assertEquals(NavigationCameraState.TRANSITION_TO_FOLLOWING, navigationCamera.state)
    }

    @Test
    fun `when overview request canceled, state does not changes to IDLE`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)
        assertEquals(NavigationCameraState.TRANSITION_TO_OVERVIEW, navigationCamera.state)
    }

    @Test
    fun `when following requested, listener notified`() {
        navigationCamera.requestNavigationCameraToFollowing(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when following requested and canceled, listener notified`() {
        navigationCamera.requestNavigationCameraToFollowing(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = true) }
    }

    @Test
    fun `when following requested during animation, listener notified`() {
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)

        navigationCamera.requestNavigationCameraToFollowing(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when following requested after animation, listener notified`() {
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        navigationCamera.requestNavigationCameraToFollowing(
            transitionEndListener = transitionEndListener,
        )

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when overview requested, listener notified`() {
        navigationCamera.requestNavigationCameraToOverview(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when overview requested and canceled, listener notified`() {
        navigationCamera.requestNavigationCameraToOverview(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = true) }
    }

    @Test
    fun `when overview requested during animation, listener notified`() {
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)

        navigationCamera.requestNavigationCameraToOverview(
            transitionEndListener = transitionEndListener,
        )

        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when overview requested after animation, listener notified`() {
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        navigationCamera.requestNavigationCameraToOverview(
            transitionEndListener = transitionEndListener,
        )

        verify(exactly = 1) { transitionEndListener.onTransitionEnd(isCanceled = false) }
    }

    @Test
    fun `when following transition ends, unregister animators`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        followingAnimatorSet.childAnimations.forEach {
            verify(exactly = 1) { cameraPlugin.unregisterAnimators(it as ValueAnimator) }
        }
    }

    @Test
    fun `when following transition ends, do a frame animation`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            followingCameraOptions,
            DEFAULT_FRAME_TRANSITION_OPT,
            followingFrameAnimatorSet,
        )
    }

    @Test
    fun `when overview transition ends, unregister animators`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        overviewAnimatorSet.childAnimations.forEach {
            verify(exactly = 1) { cameraPlugin.unregisterAnimators(it as ValueAnimator) }
        }
    }

    @Test
    fun `when overview transition ends, do a frame animation`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            overviewCameraOptions,
            DEFAULT_FRAME_TRANSITION_OPT,
            overviewFrameAnimatorSet,
        )
    }

    @Test
    fun `when IDLE and viewport data is updated, do nothing`() {
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToFollowing,
            followingCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            followingAnimatorSet,
            times = 0,
        )
        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToOverview,
            overviewCameraOptions,
            DEFAULT_STATE_TRANSITION_OPT,
            overviewAnimatorSet,
            times = 0,
        )
        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            followingCameraOptions,
            DEFAULT_FRAME_TRANSITION_OPT,
            followingFrameAnimatorSet,
            times = 0,
        )
        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            overviewCameraOptions,
            DEFAULT_FRAME_TRANSITION_OPT,
            overviewFrameAnimatorSet,
            times = 0,
        )
    }

    @Test
    fun `when following and viewport data is update, frame changes`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when overview and viewport data is update, frame changes`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when custom transition opt, following and viewport data is update, frame changes`() {
        val stateOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(500L)
            .build()
        val frameOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(250L)
            .build()

        navigationCamera.requestNavigationCameraToFollowing(
            stateOpt,
            frameOpt,
        )

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToFollowing,
            followingCameraOptions,
            stateOpt,
            followingAnimatorSet,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            frameOpt,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            frameTransition.frameCamera,
            frameOpt,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when custom transition opt, when overview and viewport data is update, frame changes`() {
        val stateOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(500L)
            .build()
        val frameOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(250L)
            .build()

        navigationCamera.requestNavigationCameraToOverview(
            stateOpt,
            frameOpt,
        )

        verifyTransitionExecuted(
            NavigationCameraStateTransition::transitionToOverview,
            overviewCameraOptions,
            stateOpt,
            overviewAnimatorSet,
        )

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            frameOpt,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            frameTransition.frameCamera,
            frameOpt,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when custom transition opt, reset, following updates use default transition`() {
        val stateOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(500L)
            .build()
        val frameOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(250L)
            .build()

        navigationCamera.requestNavigationCameraToFollowing(
            stateOpt,
            frameOpt,
        )
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)
        navigationCamera.requestNavigationCameraToIdle()
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when custom transition opt, reset, overview updates use default transition`() {
        val stateOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(500L)
            .build()
        val frameOpt = NavigationCameraTransitionOptions.Builder()
            .maxDuration(250L)
            .build()

        navigationCamera.requestNavigationCameraToOverview(
            stateOpt,
            frameOpt,
        )
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)
        navigationCamera.requestNavigationCameraToIdle()
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
        )
    }

    @Test
    fun `when following frame ends, unregister animators`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)
        internalFrameListenerSlot.captured.onAnimationStart(frameTransition.frameAnimatorSet)
        internalFrameListenerSlot.captured.onAnimationEnd(frameTransition.frameAnimatorSet)

        frameTransition.frameAnimatorSet.childAnimations.forEach {
            verify(exactly = 1) { cameraPlugin.unregisterAnimators(it as ValueAnimator) }
        }
    }

    @Test
    fun `when overview frame ends, unregister animators`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)
        internalFrameListenerSlot.captured.onAnimationStart(frameTransition.frameAnimatorSet)
        internalFrameListenerSlot.captured.onAnimationEnd(frameTransition.frameAnimatorSet)

        frameTransition.frameAnimatorSet.childAnimations.forEach {
            verify(exactly = 1) { cameraPlugin.unregisterAnimators(it as ValueAnimator) }
        }
    }

    @Test
    fun `when overview and frame reset, instant update`() {
        navigationCamera.requestNavigationCameraToOverview()

        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        navigationCamera.resetFrame()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForOverview,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
            instant = true,
        )
    }

    @Test
    fun `when following and frame reset, instant update`() {
        navigationCamera.requestNavigationCameraToFollowing()

        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        navigationCamera.resetFrame()

        verifyTransitionExecuted(
            NavigationCameraStateTransition::updateFrameForFollowing,
            frameTransition.frameCamera,
            DEFAULT_FRAME_TRANSITION_OPT,
            frameTransition.frameAnimatorSet,
            instant = true,
        )
    }

    @Test
    fun `state following requested, changed listener gets notified`() {
        val listener = mockk<NavigationCameraStateChangedObserver>(relaxUnitFun = true)
        navigationCamera.registerNavigationCameraStateChangeObserver(listener)

        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verifySequence {
            listener.onNavigationCameraStateChanged(NavigationCameraState.IDLE)
            listener.onNavigationCameraStateChanged(NavigationCameraState.TRANSITION_TO_FOLLOWING)
            listener.onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)
        }
    }

    @Test
    fun `state following requested and canceled, changed listener gets notified`() {
        val listener = mockk<NavigationCameraStateChangedObserver>(relaxUnitFun = true)
        navigationCamera.registerNavigationCameraStateChangeObserver(listener)

        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verifySequence {
            listener.onNavigationCameraStateChanged(NavigationCameraState.IDLE)
            listener.onNavigationCameraStateChanged(NavigationCameraState.TRANSITION_TO_FOLLOWING)
        }
    }

    @Test
    fun `state overview requested, changed listener gets notified`() {
        val listener = mockk<NavigationCameraStateChangedObserver>(relaxUnitFun = true)
        navigationCamera.registerNavigationCameraStateChangeObserver(listener)

        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verifySequence {
            listener.onNavigationCameraStateChanged(NavigationCameraState.IDLE)
            listener.onNavigationCameraStateChanged(NavigationCameraState.TRANSITION_TO_OVERVIEW)
            listener.onNavigationCameraStateChanged(NavigationCameraState.OVERVIEW)
        }
    }

    @Test
    fun `state overview requested and canceled, changed listener gets notified`() {
        val listener = mockk<NavigationCameraStateChangedObserver>(relaxUnitFun = true)
        navigationCamera.registerNavigationCameraStateChangeObserver(listener)

        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verifySequence {
            listener.onNavigationCameraStateChanged(NavigationCameraState.IDLE)
            listener.onNavigationCameraStateChanged(NavigationCameraState.TRANSITION_TO_OVERVIEW)
        }
    }

    @Test
    fun `unregistering state change observer stops notifications`() {
        val listener = mockk<NavigationCameraStateChangedObserver>(relaxUnitFun = true)
        navigationCamera.registerNavigationCameraStateChangeObserver(listener)
        navigationCamera.unregisterNavigationCameraStateChangeObserver(listener)

        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationCancel(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verify(exactly = 1) { listener.onNavigationCameraStateChanged(NavigationCameraState.IDLE) }
    }

    @Test
    fun `when following transition and idle requested, cancel animations and change state`() {
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)

        navigationCamera.requestNavigationCameraToIdle()
        internalTransitionListenerSlot.captured.onAnimationCancel(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        verify(exactly = 1) { followingAnimatorSet.cancel() }
        assertEquals(NavigationCameraState.IDLE, navigationCamera.state)
    }

    @Test
    fun `when overview transition and idle requested, cancel animations and change state`() {
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)

        navigationCamera.requestNavigationCameraToIdle()
        internalTransitionListenerSlot.captured.onAnimationCancel(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        verify(exactly = 1) { overviewAnimatorSet.cancel() }
        assertEquals(NavigationCameraState.IDLE, navigationCamera.state)
    }

    @Test
    fun `when following and idle requested, cancel animations and change state`() {
        navigationCamera.requestNavigationCameraToFollowing()
        internalTransitionListenerSlot.captured.onAnimationStart(followingAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(followingAnimatorSet)

        val frameTransition = mockFrameTransitionForFollowing(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        navigationCamera.requestNavigationCameraToIdle()
        internalFrameListenerSlot.captured.onAnimationCancel(frameTransition.frameAnimatorSet)
        internalFrameListenerSlot.captured.onAnimationEnd(frameTransition.frameAnimatorSet)

        verify(exactly = 1) { frameTransition.frameAnimatorSet.cancel() }
        assertEquals(NavigationCameraState.IDLE, navigationCamera.state)
    }

    @Test
    fun `when overview and idle requested, cancel animations and change state`() {
        navigationCamera.requestNavigationCameraToOverview()
        internalTransitionListenerSlot.captured.onAnimationStart(overviewAnimatorSet)
        internalTransitionListenerSlot.captured.onAnimationEnd(overviewAnimatorSet)

        val frameTransition = mockFrameTransitionForOverview(
            DEFAULT_FRAME_TRANSITION_OPT,
        )
        internalDataSourceObserverSlot.captured.viewportDataSourceUpdated(viewportData)

        navigationCamera.requestNavigationCameraToIdle()
        internalFrameListenerSlot.captured.onAnimationCancel(frameTransition.frameAnimatorSet)
        internalFrameListenerSlot.captured.onAnimationEnd(frameTransition.frameAnimatorSet)

        verify(exactly = 1) { frameTransition.frameAnimatorSet.cancel() }
        assertEquals(NavigationCameraState.IDLE, navigationCamera.state)
    }

    private fun verifyTransitionExecuted(
        transitionFun: NavigationCameraStateTransition.(
            CameraOptions,
            NavigationCameraTransitionOptions,
        ) -> AnimatorSet,
        transitionCameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
        returnTransitionSet: AnimatorSet,
        instant: Boolean = false,
        times: Int = 1,
    ) {
        verify(exactly = times) {
            stateTransition.transitionFun(transitionCameraOptions, transitionOptions)
        }
        returnTransitionSet.childAnimations.forEach {
            verify(exactly = times) { cameraPlugin.registerAnimators(it as ValueAnimator) }
        }
        verify(exactly = times) { returnTransitionSet.start() }
        verify(exactly = if (instant) 1 else 0) { returnTransitionSet.duration = 0 }
    }

    private fun mockFrameTransitionForFollowing(
        transitionOptions: NavigationCameraTransitionOptions,
    ): MockFrameTransition {
        val frameCamera = mockk<CameraOptions>()
        val frameAnimatorSet = mockk(
            relaxUnitFun = true,
            block = frameBlock,
        )
        every { viewportData.cameraForFollowing } returns frameCamera
        every {
            stateTransition.updateFrameForFollowing(frameCamera, transitionOptions)
        } returns frameAnimatorSet

        return MockFrameTransition(frameCamera, frameAnimatorSet)
    }

    private fun mockFrameTransitionForOverview(
        transitionOptions: NavigationCameraTransitionOptions,
    ): MockFrameTransition {
        val frameCamera = mockk<CameraOptions>()
        val frameAnimatorSet = mockk(
            relaxUnitFun = true,
            block = frameBlock,
        )
        every { viewportData.cameraForOverview } returns frameCamera
        every {
            stateTransition.updateFrameForOverview(frameCamera, transitionOptions)
        } returns frameAnimatorSet

        return MockFrameTransition(frameCamera, frameAnimatorSet)
    }

    private data class MockFrameTransition(
        val frameCamera: CameraOptions,
        val frameAnimatorSet: AnimatorSet,
    )
}
