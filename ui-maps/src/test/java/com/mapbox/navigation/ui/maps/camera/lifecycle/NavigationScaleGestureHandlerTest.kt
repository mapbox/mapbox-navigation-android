package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import android.graphics.RectF
import com.mapbox.android.gestures.AndroidGesturesManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCoalescedCallback
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.invoke

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
    private val initialMoveGestureDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
    private val initialShoveGestureDetector: ShoveGestureDetector = mockk(relaxUnitFun = true)
    private val customGesturesManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
    private val customMoveGestureDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
    private val customRotateGestureDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
    private val customShoveDetector: ShoveGestureDetector = mockk(relaxUnitFun = true)
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
    private val onShoveListenerSlot = slot<OnShoveListener>()
    private val onIndicatorPositionChangedListenerSlot = slot<OnIndicatorPositionChangedListener>()

    @OptIn(MapboxExperimental::class)
    private val cameraChangedCallbackSlot = slot<CameraChangedCoalescedCallback>()
    private val cameraChangedTask = mockk<Cancelable>(relaxed = true)
    private val navigationCameraStateChangedObserverSlot =
        slot<NavigationCameraStateChangedObserver>()
    private val initialMoveThreshold = 1.0f
    private val initialMoveThresdholdRect = RectF(1.0f, 1.0f, 1.0f, 1.0f)
    private val initialShoveDeltaThreshold = 2.0f

    @Before
    fun setup() {
        mockkObject(NavigationCameraLifecycleProvider)
        every { navigationCamera.state } returns NavigationCameraState.IDLE
        every {
            NavigationCameraLifecycleProvider.getCustomGesturesManager(
                context,
                capture(customGesturesInteractorSlot),
            )
        } returns customGesturesManager
        every { customGesturesManager.moveGestureDetector } returns customMoveGestureDetector
        every { customMoveGestureDetector.pointersCount } returns 1
        every { customMoveGestureDetector.moveThreshold } returns initialMoveThreshold
        every { customMoveGestureDetector.moveThresholdRect } returns initialMoveThresdholdRect
        every { customGesturesManager.rotateGestureDetector } returns customRotateGestureDetector
        every { customGesturesManager.shoveGestureDetector } returns customShoveDetector
        every { customShoveDetector.pointersCount } returns 2
        every { customRotateGestureDetector.angleThreshold } returns 3.0f
        every { customShoveDetector.pixelDeltaThreshold } returns initialShoveDeltaThreshold
        every { gesturesPlugin.getGesturesManager() } returns initialGesturesManager
        every { initialGesturesManager.rotateGestureDetector } returns initialRotateGestureDetector
        every { initialRotateGestureDetector.angleThreshold } returns 3.0f
        every { initialGesturesManager.moveGestureDetector } returns initialMoveGestureDetector
        every { initialMoveGestureDetector.pointersCount } returns 1
        every { initialGesturesManager.shoveGestureDetector } returns initialShoveGestureDetector
        every { initialShoveGestureDetector.pointersCount } returns 2
        every { initialMoveGestureDetector.moveThreshold } returns initialMoveThreshold
        every { initialMoveGestureDetector.moveThresholdRect } returns initialMoveThresdholdRect
        every { initialShoveGestureDetector.pixelDeltaThreshold } returns initialShoveDeltaThreshold
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
        every { gesturesPlugin.addOnShoveListener(capture(onShoveListenerSlot)) } just Runs
        every {
            locationPlugin.addOnIndicatorPositionChangedListener(
                capture(onIndicatorPositionChangedListenerSlot),
            )
        } just Runs
        every {
            @OptIn(MapboxExperimental::class)
            mapboxMap.subscribeCameraChangedCoalesced(capture(cameraChangedCallbackSlot))
        } returns cameraChangedTask
        every {
            navigationCamera.registerNavigationCameraStateChangeObserver(
                capture(navigationCameraStateChangedObserverSlot),
            )
        } just Runs

        LoggerProvider.setLoggerFrontend(mockk(relaxed = true))

        controller = NavigationScaleGestureHandler(
            context,
            navigationCamera,
            mapboxMap,
            gesturesPlugin,
            locationPlugin,
            scaleActionListener,
            options,
            MutableStateFlow(true),
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
            customShoveDetector.pixelDeltaThreshold = options.followingMultiFingerMoveThreshold
        }
    }

    @Test
    fun `when camera state changed NOT to following state`() {
        controller.initialize()
        every { navigationCamera.state } returns NavigationCameraState.IDLE

        navigationCameraStateChangedObserverSlot.captured
            .onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)

        verify(exactly = 1) {
            customMoveGestureDetector.moveThreshold = initialMoveThreshold
            customMoveGestureDetector.moveThresholdRect = initialMoveThresdholdRect
            customRotateGestureDetector.angleThreshold = 3.0f
            customShoveDetector.pixelDeltaThreshold = initialShoveDeltaThreshold
        }
    }

    @Test
    fun `when two handlers exist only active handler adjusts thresholds`() {
        val activeNavigationCamera: NavigationCamera = mockk(relaxUnitFun = true)
        val inactiveNavigationCamera: NavigationCamera = mockk(relaxUnitFun = true)
        every { activeNavigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { inactiveNavigationCamera.state } returns NavigationCameraState.FOLLOWING

        val sharedGesturesPlugin: GesturesPlugin = mockk(relaxUnitFun = true)
        val initialManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
        val initialMoveDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
        val initialRotateDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
        val initialShoveDetector: ShoveGestureDetector = mockk(relaxUnitFun = true)
        every { initialManager.moveGestureDetector } returns initialMoveDetector
        every { initialManager.rotateGestureDetector } returns initialRotateDetector
        every { initialManager.shoveGestureDetector } returns initialShoveDetector
        every { initialMoveDetector.moveThreshold } returns initialMoveThreshold
        every { initialMoveDetector.moveThresholdRect } returns initialMoveThresdholdRect
        every { initialRotateDetector.angleThreshold } returns 3.0f
        every { initialShoveDetector.pixelDeltaThreshold } returns initialShoveDeltaThreshold
        every { sharedGesturesPlugin.getGesturesManager() } returns initialManager

        val activeCustomManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
        val activeMoveDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
        val activeRotateDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
        val activeShoveDetector: ShoveGestureDetector = mockk(relaxUnitFun = true)
        every { activeCustomManager.moveGestureDetector } returns activeMoveDetector
        every { activeCustomManager.rotateGestureDetector } returns activeRotateDetector
        every { activeCustomManager.shoveGestureDetector } returns activeShoveDetector

        val inactiveCustomManager: AndroidGesturesManager = mockk(relaxUnitFun = true)
        val inactiveMoveDetector: MoveGestureDetector = mockk(relaxUnitFun = true)
        val inactiveRotateDetector: RotateGestureDetector = mockk(relaxUnitFun = true)
        val inactiveShoveDetector: ShoveGestureDetector = mockk(relaxUnitFun = true)
        every { inactiveCustomManager.moveGestureDetector } returns inactiveMoveDetector
        every { inactiveCustomManager.rotateGestureDetector } returns inactiveRotateDetector
        every { inactiveCustomManager.shoveGestureDetector } returns inactiveShoveDetector

        every {
            NavigationCameraLifecycleProvider.getCustomGesturesManager(context, any())
        } returnsMany listOf(activeCustomManager, inactiveCustomManager)

        val activeObserverSlot = slot<NavigationCameraStateChangedObserver>()
        val inactiveObserverSlot = slot<NavigationCameraStateChangedObserver>()
        every {
            activeNavigationCamera.registerNavigationCameraStateChangeObserver(
                capture(activeObserverSlot),
            )
        } just Runs
        every {
            inactiveNavigationCamera.registerNavigationCameraStateChangeObserver(
                capture(inactiveObserverSlot),
            )
        } just Runs

        val activeLocationPlugin: LocationComponentPlugin = mockk(relaxUnitFun = true)
        val inactiveLocationPlugin: LocationComponentPlugin = mockk(relaxUnitFun = true)
        every {
            mapboxMap.subscribeCameraChangedCoalesced(any())
        } returns mockk(relaxed = true)

        val activeHandler = NavigationScaleGestureHandler(
            context,
            activeNavigationCamera,
            mapboxMap,
            sharedGesturesPlugin,
            activeLocationPlugin,
            scaleActionListener,
            options,
            MutableStateFlow(true),
        )
        val inactiveHandler = NavigationScaleGestureHandler(
            context,
            inactiveNavigationCamera,
            mapboxMap,
            sharedGesturesPlugin,
            inactiveLocationPlugin,
            scaleActionListener,
            options,
            MutableStateFlow(false),
        )

        activeHandler.initialize()
        inactiveHandler.initialize()
        activeObserverSlot.captured.onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)
        inactiveObserverSlot.captured.onNavigationCameraStateChanged(
            NavigationCameraState.FOLLOWING,
        )

        verify(exactly = 1) {
            activeMoveDetector.moveThreshold = options.followingInitialMoveThreshold
        }
        verify(exactly = 1) {
            activeMoveDetector.moveThresholdRect = options.followingMultiFingerProtectedMoveArea
        }
        verify(exactly = 1) {
            activeRotateDetector.angleThreshold = options.followingRotationAngleThreshold
        }
        verify(exactly = 1) {
            activeShoveDetector.pixelDeltaThreshold = options.followingMultiFingerMoveThreshold
        }

        verify(exactly = 0) { inactiveMoveDetector.moveThreshold = any() }
        verify(exactly = 0) { inactiveMoveDetector.moveThresholdRect = any() }
        verify(exactly = 0) { inactiveRotateDetector.angleThreshold = any() }
        verify(exactly = 0) { inactiveShoveDetector.pixelDeltaThreshold = any() }
    }

    @Test
    fun `when other gesture animations started, request idle`() {
        controller.initialize()
        val types =
            CameraAnimatorType
                .values()
                .toMutableList().apply {
                    removeAll(
                        listOf(
                            CameraAnimatorType.ZOOM,
                            CameraAnimatorType.ANCHOR,
                            CameraAnimatorType.CENTER,
                        ),
                    )
                }
        for (type in types) {
            clearMocks(navigationCamera, answers = false)
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
    fun `when up motion event and not following, reset gesture thresholds`() {
        controller.initialize()
        val states =
            NavigationCameraState
                .values()
                .toMutableList()
                .apply { remove(NavigationCameraState.FOLLOWING) }
        for (state in states) {
            clearMocks(customMoveGestureDetector, answers = false)
            every { navigationCamera.state } returns state
            customGesturesInteractorSlot.captured.invoke(customGesturesManager)

            verify(exactly = 1) { customMoveGestureDetector.moveThreshold = initialMoveThreshold }
            verify(exactly = 1) {
                customMoveGestureDetector.moveThresholdRect = initialMoveThresdholdRect
            }
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
            clearMocks(navigationCamera, answers = false)
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
        clearMocks(customMoveGestureDetector, answers = false)
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
    fun `when not following and shove gestures executed, request idle`() {
        controller.initialize()
        val states =
            NavigationCameraState
                .values()
                .toMutableList()
                .apply { remove(NavigationCameraState.FOLLOWING) }
        for (state in states) {
            clearMocks(navigationCamera, answers = false)
            every { navigationCamera.state } returns state
            onShoveListenerSlot.captured.onShoveBegin(customShoveDetector)

            verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
        }
    }

    @Test
    fun `when following and shove gesture threshold is adjusted, interrupt gesture`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customShoveDetector.pixelDeltaThreshold } returns 0f

        controller.initialize()
        onShoveListenerSlot.captured.onShoveBegin(customShoveDetector)
        onShoveListenerSlot.captured.onShove(customShoveDetector)

        verify(exactly = 1) {
            customShoveDetector.pixelDeltaThreshold = options.followingMultiFingerMoveThreshold
        }
        verify(exactly = 1) { customShoveDetector.interrupt() }
        verify(exactly = 0) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when following and shove gesture threshold is already adjusted, request idle`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every {
            customShoveDetector.pixelDeltaThreshold
        } returns options.followingMultiFingerMoveThreshold

        controller.initialize()
        onShoveListenerSlot.captured.onShoveBegin(customShoveDetector)
        onShoveListenerSlot.captured.onShove(customShoveDetector)

        verify(exactly = 1) { navigationCamera.requestNavigationCameraToIdle() }
    }

    @Test
    fun `when interrupted shove gesture finishes, do nothing`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every { customShoveDetector.pixelDeltaThreshold } returns 0f

        controller.initialize()
        onShoveListenerSlot.captured.onShoveBegin(customShoveDetector)
        onShoveListenerSlot.captured.onShove(customShoveDetector)
        clearMocks(customShoveDetector)
        onShoveListenerSlot.captured.onShoveEnd(customShoveDetector)

        verify(exactly = 0) { customShoveDetector.pixelDeltaThreshold = any() }
    }

    @Test
    fun `when shove gesture finishes, readjust threshold`() {
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING
        every {
            customShoveDetector.pixelDeltaThreshold
        } returns options.followingMultiFingerMoveThreshold

        controller.initialize()
        onShoveListenerSlot.captured.onShoveBegin(customShoveDetector)
        onShoveListenerSlot.captured.onShove(customShoveDetector)
        onShoveListenerSlot.captured.onShoveEnd(customShoveDetector)

        verify(exactly = 1) {
            customShoveDetector.pixelDeltaThreshold = options.followingMultiFingerMoveThreshold
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
    fun `when camera state changed to following, disable fling deceleration`() {
        val gesturesSettings = GesturesSettings.Builder().setScrollDecelerationEnabled(true)
        every { gesturesPlugin.updateSettings(captureLambda()) } answers {
            lambda<GesturesSettings.Builder.() -> Unit>().invoke(gesturesSettings)
        }

        controller.initialize()
        every { navigationCamera.state } returns NavigationCameraState.FOLLOWING

        navigationCameraStateChangedObserverSlot.captured
            .onNavigationCameraStateChanged(NavigationCameraState.FOLLOWING)

        assertFalse(gesturesSettings.scrollDecelerationEnabled)
        verify(exactly = 1) { gesturesPlugin.updateSettings(any()) }
    }

    @Test
    fun `when camera state changed to non-following, enable fling deceleration`() {
        val gesturesSettings = GesturesSettings.Builder().setScrollDecelerationEnabled(false)
        every { gesturesPlugin.updateSettings(captureLambda()) } answers {
            lambda<GesturesSettings.Builder.() -> Unit>().invoke(gesturesSettings)
        }

        controller.initialize()
        every { navigationCamera.state } returns NavigationCameraState.IDLE

        navigationCameraStateChangedObserverSlot.captured
            .onNavigationCameraStateChanged(NavigationCameraState.IDLE)

        assertTrue(gesturesSettings.scrollDecelerationEnabled)
        verify(exactly = 1) { gesturesPlugin.updateSettings(any()) }
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
        verify(exactly = 1) { gesturesPlugin.removeOnShoveListener(onShoveListenerSlot.captured) }
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
