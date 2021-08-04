package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.annotation.UiThread
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.ViewportData
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceUpdateObserver
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.FOLLOWING
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.IDLE
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.OVERVIEW
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.TRANSITION_TO_FOLLOWING
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.TRANSITION_TO_OVERVIEW
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.transition.MapboxNavigationCameraStateTransition
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransition
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import java.util.concurrent.CopyOnWriteArraySet

/**
 * `NavigationCamera` is a class that tries to simplify management of the Map's camera object in
 * typical navigation scenarios. It's fed camera frames via the [ViewportDataSource],
 * generates transitions with [NavigationCameraStateTransition] and executes them.
 *
 * `NavigationCamera`'s lifecycle can't exceed the lifecycle of
 * the [MapboxMap] (or indirectly [MapView]) that it's attached to without risking reference leaks.
 *
 * ## States
 * The `NavigationCamera` is an entity that offers to maintain 3 distinct [NavigationCameraState]s:
 * [IDLE], [FOLLOWING], and [OVERVIEW]. States can be requested at any point in runtime.
 *
 * When the camera is transitioning between states, it reports that status with
 * [TRANSITION_TO_FOLLOWING] and [TRANSITION_TO_OVERVIEW] helper states.
 * These helper transition states cannot be directly requested.
 *
 * Change to [IDLE] state is always instantaneous.
 *
 * ## Data
 * In order to be able to perform state transitions or later frame updates,
 * the `NavigationCamera` needs data. This is provided by the [ViewportDataSource] argument.
 * The source is an observable interface that produces `CameraOptions` that frame the camera
 * for both [FOLLOWING] and [OVERVIEW] states.
 *
 * On creation, `NavigationCamera` subscribes to the data source and listens for updates.
 *
 * [MapboxNavigationViewportDataSource] is a default implementation of the source that helps to
 * generate camera frames based on the current route’s geometry, road's graph, trip's progress, etc.
 *
 * ## Transitions
 * When `NavigationCamera` is supplied with data and a state request, it invokes the
 * [NavigationCameraStateTransition] that generates a set of Map SDK [CameraAnimator]s that perform
 * the transition to the desired camera position created by the data source.
 *
 * When a state is requested, `NavigationCamera` takes the latest computed [ViewportData] values
 * and passes them to the [NavigationCameraStateTransition]
 * to create the [NavigationCameraStateTransition.transitionToFollowing]
 * or [NavigationCameraStateTransition.transitionToOverview] transitions.
 *
 * When `NavigationCamera` already is in one of the [FOLLOWING] or [OVERVIEW] states,
 * data source updates trigger creation of [NavigationCameraStateTransition.updateFrameForFollowing]
 * or [NavigationCameraStateTransition.updateFrameForOverview] transitions.
 *
 * After generating the transitions, `NavigationCamera` handles registering them to Maps SDK,
 * executing, listening for cancellation, adjusting states, etc.
 *
 * ## Gestures and other camera interactions
 * When [FOLLOWING] or [OVERVIEW] states are engaged, the `NavigationCamera` assumes full ownership
 * of the [CameraAnimationsPlugin]. This means that if any other camera transition is scheduled
 * outside of the `NavigationCamera`’s context, there might be side-effects or glitches.
 * Consequently, if you want to perform other camera transitions,
 * first call [requestNavigationCameraToIdle], and only after that perform the desired transition.
 *
 * Alternatively, you can use one of the default implementations
 * of [CameraAnimationsLifecycleListener] that automate the response of the `NavigationCamera` for
 * gesture interactions and other camera animations:
 * - [NavigationBasicGesturesHandler] transitions `NavigationCamera` to [NavigationCameraState.IDLE]
 * when any camera transitions outside of the `NavigationCamera` context is started.
 * - [NavigationScaleGestureHandler] behaves as above, but allows for executing various scale
 * gestures to manipulate the camera's zoom level when in [NavigationCameraState.FOLLOWING] without
 * immediately falling back to [NavigationCameraState.IDLE].
 *
 * ## Debugging
 * If you are using the [MapboxNavigationViewportDataSource] instance,
 * you can use [debugger] to provide a [MapboxNavigationViewportDataSourceDebugger] instance
 * which will draw various info on the screen when the [NavigationCamera] operates to together with
 * the [MapboxNavigationViewportDataSource].
 *
 * Make sure to also provide the same instance to [MapboxNavigationViewportDataSource.debugger].
 */
@UiThread
class NavigationCamera(
    mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin,
    private val viewportDataSource: ViewportDataSource,
    private val stateTransition: NavigationCameraStateTransition =
        MapboxNavigationCameraStateTransition(mapboxMap, cameraPlugin)
) {

    companion object {
        /**
         * Constant used to recognize the owner of transitions initiated by the [NavigationCamera].
         *
         * @see CameraAnimator.owner
         */
        const val NAVIGATION_CAMERA_OWNER = "NAVIGATION_CAMERA_OWNER"

        internal val DEFAULT_STATE_TRANSITION_OPT =
            NavigationCameraTransitionOptions.Builder().maxDuration(3500L).build()
        internal val DEFAULT_FRAME_TRANSITION_OPT =
            NavigationCameraTransitionOptions.Builder().maxDuration(1000L).build()
    }

    private var runningAnimation: AnimatorSet? = null
    private var frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT

    private val navigationCameraStateChangedObservers =
        CopyOnWriteArraySet<NavigationCameraStateChangedObserver>()

    /**
     * Returns current [NavigationCameraState].
     * @see registerNavigationCameraStateChangeObserver
     */
    var state: NavigationCameraState = IDLE
        private set(value) {
            if (value != field) {
                field = value
                updateDebugger()
                navigationCameraStateChangedObservers.forEach {
                    it.onNavigationCameraStateChanged(value)
                }
            }
        }

    /**
     * Set a [MapboxNavigationViewportDataSourceDebugger].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    var debugger: MapboxNavigationViewportDataSourceDebugger? = null

    private val sourceUpdateObserver =
        ViewportDataSourceUpdateObserver {
            viewportData ->
            updateFrame(viewportData, instant = false)
        }

    init {
        viewportDataSource.registerUpdateObserver(sourceUpdateObserver)
    }

    /**
     * Executes a transition to [FOLLOWING] state. When started, goes to [TRANSITION_TO_FOLLOWING]
     * and to the final [FOLLOWING] when ended. If transition is canceled, state goes to [IDLE].
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptionsBlock options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptionsBlock options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [FOLLOWING] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param animatorListener invoked on transition's progress.
     */
    @JvmOverloads
    fun requestNavigationCameraToFollowing(
        stateTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        frameTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        animatorListener: Animator.AnimatorListener? = null
    ) {
        requestNavigationCameraToFollowing(
            stateTransitionOptions =
            NavigationCameraTransitionOptions.Builder().apply(stateTransitionOptionsBlock).build(),
            frameTransitionOptions =
            NavigationCameraTransitionOptions.Builder().apply(frameTransitionOptionsBlock).build(),
            animatorListener = animatorListener
        )
    }

    /**
     * Executes a transition to [FOLLOWING] state. When started, goes to [TRANSITION_TO_FOLLOWING]
     * and to the final [FOLLOWING] when ended. If transition is canceled, state goes to [IDLE].
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptions options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptions options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [FOLLOWING] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param animatorListener invoked on transition's progress.
     */
    @JvmOverloads
    fun requestNavigationCameraToFollowing(
        stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
        frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
        animatorListener: Animator.AnimatorListener? = null
    ) {
        when (state) {
            TRANSITION_TO_FOLLOWING, FOLLOWING -> {
                // this should invoke animatorListener
                // https://github.com/mapbox/mapbox-navigation-android/issues/4341
                return
            }
            IDLE, TRANSITION_TO_OVERVIEW, OVERVIEW -> {
                val data = viewportDataSource.getViewportData()
                startAnimation(
                    stateTransition.transitionToFollowing(
                        data.cameraForFollowing,
                        stateTransitionOptions
                    ).apply {
                        addListener(
                            createTransitionListener(
                                TRANSITION_TO_FOLLOWING,
                                FOLLOWING,
                                frameTransitionOptions,
                                animatorListener
                            )
                        )
                    },
                    instant = false
                )
            }
        }
    }

    /**
     * Executes a transition to [OVERVIEW] state. When started, goes to [TRANSITION_TO_OVERVIEW]
     * and to the final [OVERVIEW] when ended. If transition is canceled, state goes to [IDLE].
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptionsBlock options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptionsBlock options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [OVERVIEW] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param animatorListener invoked on transition's progress.
     */
    @JvmOverloads
    fun requestNavigationCameraToOverview(
        stateTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        frameTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        animatorListener: Animator.AnimatorListener? = null
    ) {
        requestNavigationCameraToOverview(
            stateTransitionOptions =
            NavigationCameraTransitionOptions.Builder().apply(stateTransitionOptionsBlock).build(),
            frameTransitionOptions =
            NavigationCameraTransitionOptions.Builder().apply(frameTransitionOptionsBlock).build(),
            animatorListener = animatorListener
        )
    }

    /**
     * Executes a transition to [OVERVIEW] state. When started, goes to [TRANSITION_TO_OVERVIEW]
     * and to the final [OVERVIEW] when ended. If transition is canceled, state goes to [IDLE].
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptions options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptions options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [OVERVIEW] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param animatorListener invoked on transition's progress.
     */
    @JvmOverloads
    fun requestNavigationCameraToOverview(
        stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
        frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
        animatorListener: Animator.AnimatorListener? = null
    ) {
        when (state) {
            TRANSITION_TO_OVERVIEW, OVERVIEW -> {
                // this should invoke animatorListener
                // https://github.com/mapbox/mapbox-navigation-android/issues/4341
                return
            }
            IDLE, TRANSITION_TO_FOLLOWING, FOLLOWING -> {
                val data = viewportDataSource.getViewportData()
                startAnimation(
                    stateTransition.transitionToOverview(
                        data.cameraForOverview,
                        stateTransitionOptions
                    ).apply {
                        addListener(
                            createTransitionListener(
                                TRANSITION_TO_OVERVIEW,
                                OVERVIEW,
                                frameTransitionOptions,
                                animatorListener
                            )
                        )
                    },
                    instant = false
                )
            }
        }
    }

    /**
     * Immediately goes to [IDLE] state canceling all ongoing transitions.
     */
    fun requestNavigationCameraToIdle() {
        if (state != IDLE) {
            cancelAnimation()
            setIdleProperties()
        }
    }

    /**
     * If the [state] is [FOLLOWING] or [OVERVIEW],
     * performs an immediate camera transition (a jump, with animation duration equal to `0`)
     * based on the latest data obtained with [ViewportDataSource.getViewportData].
     */
    fun resetFrame() {
        val viewportData = viewportDataSource.getViewportData()
        updateFrame(viewportData, instant = true)
    }

    private fun updateFrame(viewportData: ViewportData, instant: Boolean) {
        when (state) {
            FOLLOWING -> {
                startAnimation(
                    stateTransition.updateFrameForFollowing(
                        viewportData.cameraForFollowing,
                        frameTransitionOptions
                    ).apply {
                        addListener(createFrameListener())
                    },
                    instant
                )
            }
            OVERVIEW -> {
                startAnimation(
                    stateTransition.updateFrameForOverview(
                        viewportData.cameraForOverview,
                        frameTransitionOptions
                    ).apply {
                        addListener(createFrameListener())
                    },
                    instant
                )
            }
            IDLE, TRANSITION_TO_FOLLOWING, TRANSITION_TO_OVERVIEW -> {
                // no impl
            }
        }
    }

    /**
     * Registers [NavigationCameraStateChangedObserver].
     */
    fun registerNavigationCameraStateChangeObserver(
        navigationCameraStateChangedObserver: NavigationCameraStateChangedObserver
    ) {
        navigationCameraStateChangedObservers.add(navigationCameraStateChangedObserver)
        navigationCameraStateChangedObserver.onNavigationCameraStateChanged(state)
    }

    /**
     * Unregisters [NavigationCameraStateChangedObserver].
     */
    fun unregisterNavigationCameraStateChangeObserver(
        navigationCameraStateChangedObserver: NavigationCameraStateChangedObserver
    ) {
        navigationCameraStateChangedObservers.remove(navigationCameraStateChangedObserver)
    }

    private fun setIdleProperties() {
        this@NavigationCamera.frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT
        state = IDLE
    }

    private fun cancelAnimation() {
        runningAnimation?.let { set ->
            set.cancel()
            set.childAnimations.forEach {
                cameraPlugin.unregisterAnimators(it as ValueAnimator)
            }
        }
        runningAnimation = null
    }

    private fun startAnimation(animatorSet: AnimatorSet, instant: Boolean) {
        cancelAnimation()
        animatorSet.childAnimations.forEach {
            cameraPlugin.registerAnimators(it as ValueAnimator)
        }
        if (instant) {
            animatorSet.duration = 0
        }

        // workaround for https://github.com/mapbox/mapbox-maps-android/issues/277
        cameraPlugin.anchor = null

        animatorSet.start()
        runningAnimation = animatorSet
    }

    private fun finishAnimation(animatorSet: AnimatorSet) {
        animatorSet.childAnimations.forEach {
            cameraPlugin.unregisterAnimators(it as ValueAnimator)
        }
        if (runningAnimation == animatorSet) {
            runningAnimation = null
        }
    }

    private fun createTransitionListener(
        progressState: NavigationCameraState,
        finalState: NavigationCameraState,
        frameTransitionOptions: NavigationCameraTransitionOptions,
        externalListener: Animator.AnimatorListener?
    ) = object : Animator.AnimatorListener {

        private var isCanceled = false

        override fun onAnimationStart(animation: Animator?) {
            this@NavigationCamera.frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT
            state = progressState
            externalListener?.onAnimationStart(animation)
        }

        override fun onAnimationEnd(animation: Animator?) {
            if (isCanceled) {
                setIdleProperties()
            } else {
                this@NavigationCamera.frameTransitionOptions = frameTransitionOptions
                state = finalState
            }

            finishAnimation(animation as AnimatorSet)
            externalListener?.onAnimationEnd(animation)
            updateFrame(viewportDataSource.getViewportData(), instant = false)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (!isCanceled) {
                isCanceled = true
                externalListener?.onAnimationCancel(animation)
            }
        }

        override fun onAnimationRepeat(animation: Animator?) {
            externalListener?.onAnimationRepeat(animation)
        }
    }

    private fun createFrameListener() = object : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator?) {
            // no impl
        }

        override fun onAnimationEnd(animation: Animator?) {
            finishAnimation(animation as AnimatorSet)
        }

        override fun onAnimationCancel(animation: Animator?) {
            // no impl
        }

        override fun onAnimationRepeat(animation: Animator?) {
            // no impl
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebugger() {
        debugger?.cameraState = state
    }
}
