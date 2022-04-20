package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.animation.ValueAnimator
import android.content.Context
import com.mapbox.android.gestures.AndroidGesturesManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver

/**
 * Provides support in reacting to map gesture interaction
 * and other animations scheduled outside of the [NavigationCamera] context.
 *
 * ### Gestures recognition
 * When enabled, the handler listens for all map gestures interactions, and if that interaction
 * is changing the zoom level of the camera, it can be freely continued. This allows users to
 * change the zoom level of the camera by executing well-known gestures like:
 * - double-tapping to zoom in
 * - two-finger-tapping to zoom out
 * - quick-scaling by double-tapping and holding the finger down while moving it up and down
 * - pinching to zoom in and out (depends on [NavigationScaleGestureHandlerOptions])
 *
 * without the camera stopping to track the current user's puck and upcoming maneuver context.
 *
 * You can configure which gestures should be recognized with [GesturesSettings].
 *
 * ### Interruptions
 * Any other camera change request outside of the [NAVIGATION_CAMERA_OWNER] and scale
 * gestures ([MapAnimationOwnerRegistry.GESTURES]) will immediately make [NavigationCamera] fallback
 * to [NavigationCameraState.IDLE] state to avoid running competing animations.
 *
 * ### Interop with the [ViewportDataSource]
 * Allowing to execute scale gestures means allowing the Maps gesture recognizer to change
 * the camera's zoom level. Those zoom level updates, can interfere with camera updates that the
 * [ViewportDataSource] produces and passes to [NavigationCamera]. It's a good practice to
 * stop the data source from producing zoom level updates during and after scale gesture
 * interaction.
 *
 * This handler exposes an [NavigationScaleGestureActionListener] which is triggered
 * whenever zoom level changes due to the gesture interaction that was allowed. This callback
 * can be used to notify the [ViewportDataSource] implementation to stop producing zoom level
 * for a certain period of time.
 *
 * For example, when using the [MapboxNavigationViewportDataSource], you can call
 * [FollowingFrameOptions.zoomUpdatesAllowed] with `false` to stop producing
 * zoom level updates. You can later reset the value to `true` (after a button click, or after
 * a certain period of time). Use [MapboxNavigationViewportDataSource.options] to mutate this state.
 *
 * ### Integration
 * To enable the handler, register it via
 * [CameraAnimationsPlugin.addCameraAnimationsLifecycleListener] and call [initialize].
 * To deinitialize, call
 * [CameraAnimationsPlugin.removeCameraAnimationsLifecycleListener] and [cleanup].
 *
 * When the handler is initialized, it sets a custom [AndroidGesturesManager] to the Maps SDK
 * and starts manipulating the gesture thresholds to make the execution of scale gestures possible
 * without impacting other camera transitions. It's important to clean up afterwards to go back
 * to the initial behavior if navigation gesture handling features are not required anymore.
 */
class NavigationScaleGestureHandler(
    context: Context,
    private val navigationCamera: NavigationCamera,
    private val mapboxMap: MapboxMap,
    private val gesturesPlugin: GesturesPlugin,
    private val locationPlugin: LocationComponentPlugin,
    private val scaleActionListener: NavigationScaleGestureActionListener? = null,
    private val options: NavigationScaleGestureHandlerOptions =
        NavigationScaleGestureHandlerOptions.Builder(context).build()
) : CameraAnimationsLifecycleListener {

    /**
     * Indicates whether the handler is initialized.
     *
     * @see initialize
     * @see cleanup
     */
    var isInitialized = false
        private set

    /**
     * Called when animator is about to start.
     */
    override fun onAnimatorCancelling(
        type: CameraAnimatorType,
        animator: ValueAnimator,
        owner: String?
    ) {
        // no impl
    }

    /**
     * Called when animator is about to cancel already running animator of same [CameraAnimatorType].
     */
    override fun onAnimatorEnding(
        type: CameraAnimatorType,
        animator: ValueAnimator,
        owner: String?
    ) {
        // no impl
    }

    /**
     * Called when animator is about to end.
     */
    override fun onAnimatorInterrupting(
        type: CameraAnimatorType,
        runningAnimator: ValueAnimator,
        runningAnimatorOwner: String?,
        newAnimator: ValueAnimator,
        newAnimatorOwner: String?
    ) {
        // no impl
    }

    /**
     * Called when [ValueAnimator] is about to cancel.
     */
    override fun onAnimatorStarting(
        type: CameraAnimatorType,
        animator: ValueAnimator,
        owner: String?
    ) {
        if (!isInitialized) {
            return
        }
        when {
            owner != NAVIGATION_CAMERA_OWNER && owner != MapAnimationOwnerRegistry.GESTURES -> {
                navigationCamera.requestNavigationCameraToIdle()
            }
            owner == MapAnimationOwnerRegistry.GESTURES -> {
                when (type) {
                    CameraAnimatorType.ZOOM ->
                        scaleActionListener?.onNavigationScaleGestureAction()
                    CameraAnimatorType.ANCHOR -> {
                        // do nothing
                    } // todo why is anchor called?
                    else -> navigationCamera.requestNavigationCameraToIdle()
                }
            }
        }
    }

    /**
     * [AndroidGesturesManager] that Maps SDK was using before the handler was initialized.
     */
    val initialGesturesManager: AndroidGesturesManager = gesturesPlugin.getGesturesManager()

    /**
     * [AndroidGesturesManager] that Maps SDK is using when the handler is initialized.
     */
    val customGesturesManager: AndroidGesturesManager =
        NavigationCameraLifecycleProvider.getCustomGesturesManager(
            context
        ) { gesturesManager ->
            adjustGesturesThresholds(gesturesManager.moveGestureDetector)
        }

    private var puckScreenPosition: Point? = null

    private val onMoveListener: OnMoveListener = object : OnMoveListener {
        private var interrupt: Boolean = false
        override fun onMoveBegin(detector: MoveGestureDetector) {
            if (navigationCamera.state == NavigationCameraState.FOLLOWING) {
                if (detector.pointersCount > 1) {
                    applyMultiFingerThresholdArea(detector)
                    applyMultiFingerMoveThreshold(detector)
                } else {
                    applySingleFingerMoveThreshold(detector)
                }
            } else {
                navigationCamera.requestNavigationCameraToIdle()
            }
        }

        private fun applyMultiFingerThresholdArea(detector: MoveGestureDetector) {
            val currentRect = detector.moveThresholdRect
            if (
                currentRect != null &&
                currentRect != options.followingMultiFingerProtectedMoveArea
            ) {
                detector.moveThresholdRect = options.followingMultiFingerProtectedMoveArea
                interrupt = true
            } else if (
                currentRect == null &&
                options.followingMultiFingerProtectedMoveArea != null
            ) {
                detector.moveThresholdRect = options.followingMultiFingerProtectedMoveArea
                interrupt = true
            }
        }

        private fun applyMultiFingerMoveThreshold(detector: MoveGestureDetector) {
            if (detector.moveThreshold != options.followingMultiFingerMoveThreshold) {
                detector.moveThreshold = options.followingMultiFingerMoveThreshold
                interrupt = true
            }
        }

        private fun applySingleFingerMoveThreshold(detector: MoveGestureDetector) {
            if (detector.moveThreshold != options.followingInitialMoveThreshold) {
                detector.moveThreshold = options.followingInitialMoveThreshold
                interrupt = true
            }
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            if (interrupt) {
                detector.interrupt()
                return false
            }
            if (navigationCamera.state == NavigationCameraState.FOLLOWING) {
                navigationCamera.requestNavigationCameraToIdle()
                detector.interrupt() // todo is this needed?
            }
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            if (!interrupt && navigationCamera.state == NavigationCameraState.FOLLOWING) {
                detector.moveThreshold = options.followingInitialMoveThreshold
                detector.moveThresholdRect = null
            }
            interrupt = false
        }
    }

    private val onIndicatorPositionChangedListener =
        OnIndicatorPositionChangedListener {
                point ->
            puckScreenPosition = point.also { adjustFocalPoint(it) }
        }

    private val onCameraChangedListener = OnCameraChangeListener {
        puckScreenPosition?.let { adjustFocalPoint(it) }
    }

    private val navigationCameraStateChangedObserver =
        NavigationCameraStateChangedObserver {
            adjustGesturesThresholds(customGesturesManager.moveGestureDetector)
        }

    private fun adjustGesturesThresholds(moveGestureDetector: MoveGestureDetector) {
        if (navigationCamera.state == NavigationCameraState.FOLLOWING) {
            moveGestureDetector.moveThreshold = options.followingInitialMoveThreshold
        } else {
            moveGestureDetector.moveThreshold = 0f
            moveGestureDetector.moveThresholdRect = null
        }
    }

    private fun adjustFocalPoint(puckPosition: Point) {
        if (navigationCamera.state == NavigationCameraState.FOLLOWING) {
            val focalPoint = mapboxMap.pixelForCoordinate(puckPosition)
            gesturesPlugin.updateSettings { this.focalPoint = focalPoint }
        } else {
            gesturesPlugin.updateSettings { focalPoint = null }
        }
    }

    /**
     * Sets a custom [AndroidGesturesManager] and other listeners to the Maps SDK
     * and starts manipulating the gesture thresholds to make the execution of scale gestures
     * possible without impacting other camera transitions.
     *
     * @see cleanup
     */
    fun initialize() {
        gesturesPlugin.setGesturesManager(
            customGesturesManager,
            attachDefaultListeners = true,
            setDefaultMutuallyExclusives = true
        )
        gesturesPlugin.addOnMoveListener(onMoveListener)
        gesturesPlugin.addProtectedAnimationOwner(NAVIGATION_CAMERA_OWNER)

        locationPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

        mapboxMap.addOnCameraChangeListener(onCameraChangedListener)

        navigationCamera.registerNavigationCameraStateChangeObserver(
            navigationCameraStateChangedObserver
        )

        isInitialized = true
    }

    /**
     * Cleans up the custom [AndroidGesturesManager] and other listeners.
     *
     * @see initialize
     */
    fun cleanup() {
        gesturesPlugin.setGesturesManager(
            initialGesturesManager,
            attachDefaultListeners = true,
            setDefaultMutuallyExclusives = true
        )
        gesturesPlugin.removeOnMoveListener(onMoveListener)
        gesturesPlugin.removeProtectedAnimationOwner(NAVIGATION_CAMERA_OWNER)

        locationPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

        mapboxMap.removeOnCameraChangeListener(onCameraChangedListener)

        navigationCamera.unregisterNavigationCameraStateChangeObserver(
            navigationCameraStateChangedObserver
        )

        isInitialized = false
    }
}
