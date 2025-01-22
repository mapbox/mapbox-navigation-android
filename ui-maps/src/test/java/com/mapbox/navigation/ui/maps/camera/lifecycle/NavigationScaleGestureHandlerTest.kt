package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import com.mapbox.android.gestures.AndroidGesturesManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationScaleGestureHandlerTest {

    private val context: Context = mockk(relaxed = true) {
        every { resources } returns mockk {
            every {
                getDimension(R.dimen.mapbox_navigationCamera_trackingInitialMoveThreshold)
            } returns 10f
            every {
                getDimension(R.dimen.mapbox_navigationCamera_trackingMultiFingerMoveThreshold)
            } returns 20f
        }
    }
    private val navigationCamera: NavigationCamera = mockk(relaxUnitFun = true)
    private val mapboxMap: MapboxMap = mockk(relaxUnitFun = true)
    private val initialGesturesManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
    private val initialRotateGestureDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
    private val customGesturesManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
    private val customMoveGestureDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
    private val customRotateGestureDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
    private val gesturesPlugin: GesturesPlugin = mockk(relaxUnitFun = true)
    private val locationPlugin: LocationComponentPlugin = mockk(relaxUnitFun = true)
    private val scaleActionListener: NavigationScaleGestureActionListener =
        mockk(relaxUnitFun = true)
    private val options = NavigationScaleGestureHandlerOptions.Builder(context)
        .followingMultiFingerProtectedMoveArea(mockk())
        .followingRotationAngleThreshold(10f)
        .build()

    private lateinit var controller: NavigationScaleGestureHandler
    private val customGesturesInteractorSlot = slot<(AndroidGesturesManager) -> Unit>()
    private val onMoveListenerSlot = slot<OnMoveListener>()
    private val onIndicatorPositionChangedListenerSlot = slot<OnIndicatorPositionChangedListener>()
    private val cameraChangedCallbackSlot = slot<CameraChangedCallback>()
    private val cameraChangedTask = mockk<Cancelable>(relaxed = true)
    private val navigationCameraStateChangedObserverSlot =
        slot<NavigationCameraStateChangedObserver>()

    @Before
    fun setup() {
        mockkObject(NavigationCameraLifecycleProvider)
        every {
            NavigationCameraLifecycleProvider.getCustomGesturesManager(
                context,
                capture(customGesturesInteractorSlot),
            )
        } returns customGesturesManager
        every { customGesturesManager.moveGestureDetector } returns customMoveGestureDetector
        every { customGesturesManager.rotateGestureDetector } returns customRotateGestureDetector
        every { customRotateGestureDetector.angleThreshold } returns 3.0f
        every { gesturesPlugin.getGesturesManager() } returns initialGesturesManager
        every { initialGesturesManager.rotateGestureDetector } returns initialRotateGestureDetector
        every { initialRotateGestureDetector.angleThreshold } returns 3.0f
        val customManagerSlot = slot<AndroidGesturesManager>()
        every {
            gesturesPlugin.setGesturesManager(
                capture(customManagerSlot),
                attachDefaultListeners = true,
                setDefaultMutuallyExclusives = true,
            )
        } answers {
            every { gesturesPlugin.getGesturesManager() } returns customGesturesManager
        }
        every { gesturesPlugin.addOnMoveListener(capture(onMoveListenerSlot)) } just Runs
        every {
            locationPlugin.addOnIndicatorPositionChangedListener(
                capture(onIndicatorPositionChangedListenerSlot),
            )
        } just Runs
        every {
            mapboxMap.subscribeCameraChanged(capture(cameraChangedCallbackSlot))
        } returns cameraChangedTask
        every {
            navigationCamera.registerNavigationCameraStateChangeObserver(
                capture(navigationCameraStateChangedObserverSlot),
            )
        } just Runs

        controller = NavigationScaleGestureHandler(
            context,
            navigationCamera,
            mapboxMap,
            gesturesPlugin,
            locationPlugin,
            scaleActionListener,
            options,
        )
    }

    @Test
    fun sanity() {
        assertNotNull(controller)
    }

    @Test
    fun `when owner-less animation started, request idle`() {
        controller.initialize()
        controller.onAnimatorStarting(mockk(), mockk(), null)

        verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when non-gesture animation started, request idle`() {
        controller.initialize()
        controller.onAnimatorStarting(mockk(), mockk(), "some owner")

        verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when navigation animation started, do not request idle`() {
        controller.initialize()
        controller.onAnimatorStarting(
            mockk(),
            mockk(),
            NAVIGATION_CAMERA_OWNER,
        )

        verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when zoom or anchor gesture animation started, do not request idle`() {
        controller.initialize()
        val types = listOf(CameraAnimatorType.ZOOM, CameraAnimatorType.ANCHOR)
        for (type in types) {
            controller.onAnimatorStarting(
                type,
                mockk(),
                MapAnimationOwnerRegistry.GESTURES,
            )

            verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
        }
    }

    @Test
    fun `when camera state changed to following state`() {
        controller.initialize()
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING

        navigationCameraStateChangedObserverSlot.captured
            .onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = options.followingInitialMoveThreshold
            customRotateGestureDetector.angleThreshold = options.followingRotationAngleThreshold
        }
    }

    @Test
    fun `when camera state changed NOT to following state`() {
        controller.initialize()
        every { navigationCamera.state } returns NavigationCameraState.IDLE

        navigationCameraStateChangedObserverSlot.captured
            .onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = 0f
            customMoveGestureDetector.moveThresholdRect = null
            customRotateGestureDetector.angleThreshold = 3.0f
        }
    }

    @Test
    fun `when other gesture animations started, request idle`() {
        controller.initialize()
        val types =
            CameraAnimatorType
                .values()
                .toMutableList().apply {
                    removeAll(listOf(CameraAnimatorType.ZOOM, CameraAnimatorType.ANCHOR))
                }
        for (type in types) {
            clearMocks(navigationCamera)
            controller.onAnimatorStarting(
                type,
                mockk(),
                MapAnimationOwnerRegistry.GESTURES,
            )

            verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
        }
    }

    @Test
    fun `when zoom gesture animation started, notify zoom interaction listener`() {
        controller.initialize()
        controller.onAnimatorStarting(
            CameraAnimatorType.ZOOM,
            mockk(),
            MapAnimationOwnerRegistry.GESTURES,
        )

        verify(exactly = 1) { scaleActionListener.onNavigationScaleGestureAction() }
    }

    @Test
    fun `initial gestures manager is available`() {
        controller.initialize()

        assertEquals(initialGesturesManager, controller.initialGesturesManager)
    }

    @Test
    fun `custom gestures manager is available`() {
        controller.initialize()

        assertEquals(customGesturesManager, controller.customGesturesManager)
    }

    @Test
    fun `custom gestures manager is set`() {
        controller.initialize()

        assertEquals(gesturesPlugin.getGesturesManager(), controller.customGesturesManager)
    }

    @Test
    fun `when up motion event and following, set gesture thresholds`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING

        controller.initialize()
        customGesturesInteractorSlot.captured.invoke(customGesturesManager)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = options.followingInitialMoveThreshold
        }
    }

    @Test
    fun `when up motion event and not following, remove gesture thresholds`() {
        controller.initialize()
        val states =
            NavigationCameraState
                .values()
                .toMutableList()
                .apply { remove(NavigationCameraState.FOLLOWING) }
        for (state in states) {
            clearMocks(customMoveGestureDetector)
            every { navigationCamera.state } returns state
            customGesturesInteractorSlot.captured.invoke(customGesturesManager)

            verify(exactly = 1) { customMoveGestureDetector.moveThreshold = 0f }
            verify(exactly = 1) { customMoveGestureDetector.moveThresholdRect = null }
        }
    }

    @Test
    fun `navigation animations are protected in the gestures plugin`() {
        controller.initialize()

        verify(exactly = 1) {
            gesturesPlugin.addProtectedAnimationOwner(
                NAVIGATION_CAMERA_OWNER,
            )
        }
    }

    @Test
    fun `when not following and move gestures executed, request idle`() {
        controller.initialize()
        val states =
            NavigationCameraState
                .values()
                .toMutableList()
                .apply { remove(NavigationCameraState.FOLLOWING) }
        for (state in states) {
            clearMocks(navigationCamera)
            every { navigationCamera.state } returns state
            onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)

            verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
        }
    }

    @Test
    fun `when following and single move gestures executed, set threshold`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 1
        every { customMoveGestureDetector.moveThreshold } returns 0f

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = options.followingInitialMoveThreshold
        }
    }

    @Test
    fun `when following and multi move gestures executed, set threshold`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 2
        every { customMoveGestureDetector.moveThreshold } returns 0f
        every { customMoveGestureDetector.moveThresholdRect } returns null

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = options.followingMultiFingerMoveThreshold
        }
        verify(exactly = 1) {
            customMoveGestureDetector.moveThresholdRect =
                options.followingMultiFingerProtectedMoveArea
        }
    }

    @Test
    fun `when following and single move gestures that adjusts thresholds, interrupt gesture`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 1
        every { customMoveGestureDetector.moveThreshold } returns 0f

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)

        verify(exactly = 1) { customMoveGestureDetector.interrupt() }
        verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when following and multi move gestures that adjusts thresholds, interrupt gesture`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 2
        every { customMoveGestureDetector.moveThreshold } returns 0f
        every { customMoveGestureDetector.moveThresholdRect } returns null

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)

        verify(exactly = 1) { customMoveGestureDetector.interrupt() }
        verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when following and single move gestures when thresholds adjusted, request idle`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 1
        every {
            customMoveGestureDetector.moveThreshold
        } returns options.followingInitialMoveThreshold

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)

        verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when following and multi move gestures when thresholds adjusted, request idle`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 2
        every {
            customMoveGestureDetector.moveThreshold
        } returns options.followingMultiFingerMoveThreshold
        every {
            customMoveGestureDetector.moveThresholdRect
        } returns options.followingMultiFingerProtectedMoveArea

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)

        verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when interrupted move gesture finishes, do nothing`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 1
        every { customMoveGestureDetector.moveThreshold } returns 0f

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)
        clearMocks(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMoveEnd(customMoveGestureDetector)

        verify(exactly = 0) { customMoveGestureDetector.moveThreshold = any() }
        verify(exactly = 0) { customMoveGestureDetector.moveThresholdRect = any() }
    }

    @Test
    fun `when move gesture finishes, readjust thresholds`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customMoveGestureDetector.pointersCount } returns 1
        every {
            customMoveGestureDetector.moveThreshold
        } returns options.followingInitialMoveThreshold

        controller.initialize()
        onMoveListenerSlot.captured.onMoveBegin(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMove(customMoveGestureDetector)
        onMoveListenerSlot.captured.onMoveEnd(customMoveGestureDetector)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = options.followingInitialMoveThreshold
        }
        verify(exactly = 1) {
            customMoveGestureDetector.moveThresholdRect = null
        }
    }

    @Test
    fun `when following and location indicator position changed, adjust focal point`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        val point = Point.fromLngLat(10.0, 20.0)
        val pixel = ScreenCoordinate(123.0, 456.0)
        every { mapboxMap.pixelForCoordinate(point) } returns pixel
        val gesturesSettings: GesturesSettings.Builder = GesturesSettings.Builder()
        every { gesturesPlugin.updateSettings(captureLambda()) } answers {
            lambda<GesturesSettings.Builder.() -> Unit>().invoke(gesturesSettings)
        }

        controller.initialize()
        onIndicatorPositionChangedListenerSlot.captured.onIndicatorPositionChanged(point)

        assertEquals(pixel, gesturesSettings.focalPoint)
    }

    @Test
    fun `when not following and location indicator position changed, adjust focal point`() {
        controller.initialize()
        val states =
            NavigationCameraState
                .values()
                .toMutableList()
                .apply { remove(NavigationCameraState.FOLLOWING) }
        for (state in states) {
            clearMocks(gesturesPlugin)
            every { navigationCamera.state } returns state
            val point = Point.fromLngLat(10.0, 20.0)
            val pixel = ScreenCoordinate(123.0, 456.0)
            every { mapboxMap.pixelForCoordinate(point) } returns pixel
            val gesturesSettings: GesturesSettings.Builder =
                GesturesSettings.Builder().setFocalPoint(mockk())
            every { gesturesPlugin.updateSettings(captureLambda()) } answers {
                lambda<GesturesSettings.Builder.() -> Unit>().invoke(gesturesSettings)
            }

            onIndicatorPositionChangedListenerSlot.captured.onIndicatorPositionChanged(point)

            assertNull(gesturesSettings.focalPoint)
        }
    }

    @Test
    fun cleanup() {
        controller.initialize()

        assertTrue(controller.isInitialized)

        controller.cleanup()

        verify(exactly = 1) {
            gesturesPlugin.setGesturesManager(
                initialGesturesManager,
                attachDefaultListeners = true,
                setDefaultMutuallyExclusives = true,
            )
        }
        verify(exactly = 1) { gesturesPlugin.removeOnMoveListener(onMoveListenerSlot.captured) }
        verify(exactly = 1) {
            gesturesPlugin.removeProtectedAnimationOwner(NAVIGATION_CAMERA_OWNER)
        }
        verify(exactly = 1) {
            locationPlugin.removeOnIndicatorPositionChangedListener(
                onIndicatorPositionChangedListenerSlot.captured,
            )
        }
        verify(exactly = 1) {
            cameraChangedTask.cancel()
        }
        verify(exactly = 1) {
            navigationCamera.unregisterNavigationCameraStateChangeObserver(
                navigationCameraStateChangedObserverSlot.captured,
            )
        }
        assertFalse(controller.isInitialized)
    }
}
