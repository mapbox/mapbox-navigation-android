package com.mapbox.navigation.ui.maps.camera

import android.os.SystemClock
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.maps.CameraAnimationHint
import com.mapbox.maps.CameraAnimationHintStage
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
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
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.transition.AnimatorsCreator
import com.mapbox.navigation.ui.maps.camera.transition.DefaultSimplifiedUpdateFrameTransitionProvider
import com.mapbox.navigation.ui.maps.camera.transition.FullAnimatorSet
import com.mapbox.navigation.ui.maps.camera.transition.FullFrameAnimatorsCreator
import com.mapbox.navigation.ui.maps.camera.transition.MapboxAnimatorSet
import com.mapbox.navigation.ui.maps.camera.transition.MapboxAnimatorSetListener
import com.mapbox.navigation.ui.maps.camera.transition.MapboxNavigationCameraStateTransition
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransition
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransitionProvider
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransitionWrapper
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.camera.transition.SimplifiedFrameAnimatorsCreator
import com.mapbox.navigation.ui.maps.camera.transition.TransitionEndListener
import com.mapbox.navigation.ui.maps.camera.transition.UpdateFrameAnimatorsOptions
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateChangedObserverInternal
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.FOLLOWING
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.IDLE
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.POINTS_OVERVIEW
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.ROUTE_OVERVIEW
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.TRANSITION_TO_FOLLOWING
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.TRANSITION_TO_POINTS_OVERVIEW
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal.TRANSITION_TO_ROUTE_OVERVIEW
import com.mapbox.navigation.ui.maps.internal.camera.SimplifiedUpdateFrameTransitionProvider
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
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
 * [NavigationCameraState.IDLE], [NavigationCameraState.FOLLOWING], and [NavigationCameraState.OVERVIEW]. States can be requested at any point in runtime.
 *
 * When the camera is transitioning between states, it reports that status with
 * [NavigationCameraState.TRANSITION_TO_FOLLOWING] and [NavigationCameraState.TRANSITION_TO_OVERVIEW] helper states.
 * These helper transition states cannot be directly requested.
 *
 * Change to [NavigationCameraState.IDLE] state is always instantaneous.
 *
 * ## Data
 * In order to be able to perform state transitions or later frame updates,
 * the `NavigationCamera` needs data. This is provided by the [ViewportDataSource] argument.
 * The source is an observable interface that produces `CameraOptions` that frame the camera
 * for both [NavigationCameraState.FOLLOWING] and [NavigationCameraState.OVERVIEW] states.
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
 * When `NavigationCamera` already is in one of the [NavigationCameraState.FOLLOWING] or [NavigationCameraState.OVERVIEW] states,
 * data source updates trigger creation of [NavigationCameraStateTransition.updateFrameForFollowing]
 * or [NavigationCameraStateTransition.updateFrameForOverview] transitions.
 *
 * After generating the transitions, `NavigationCamera` handles registering them to Maps SDK,
 * executing, listening for cancellation, adjusting states, etc.
 *
 * ## Gestures and other camera interactions
 * The `NavigationCamera` assumes full ownership of the [CameraAnimationsPlugin]. This means that
 * if any other camera transition is scheduled outside of the `NavigationCamera`’s context, there
 * might be side-effects or glitches. Consequently, if you want to perform other camera transitions,
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
 * ### Frame transitions
 * By default, NavSDK supports any type of dependencies between animators in a compound frame update animation.
 * Meaning that center, zoom, padding, pitch and bearing animators can form any dependencies graph supported by [AnimatorSet] API.
 * However, this may poorly influence the performance.
 * If you pass `updateFrameAnimatorsOptions` with [UpdateFrameAnimatorsOptions.useSimplifiedAnimatorsDependency] set to true,
 * NavSDK will assume the following restrictions for update frame animations:
 * 1. They are played together (started at the same time);
 * 2. They don't have start delays.
 * Note 1: they can still be of different duration.
 * Note 2: this is ony relevant for update frame animations. For state transition animations (`NavigationCameraStateTransition#transitionToFollowing` and `NavigationCameraStateTransition#transitionToOverview`) no such assumptions are made.
 * This allows NavSDK to execute the animations in a more performant way.
 * If this simplified setup works for you (it's especially important to check these conditions if you use custom [NavigationCameraStateTransition]),
 * you can set [UpdateFrameAnimatorsOptions.useSimplifiedAnimatorsDependency] to true for simpler, but more optimized update frame animations.
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
class NavigationCamera
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin,
    private val viewportDataSource: ViewportDataSource,
    private val animatorsCreator: AnimatorsCreator,
) {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    constructor(
        mapboxMap: MapboxMap,
        cameraPlugin: CameraAnimationsPlugin,
        viewportDataSource: ViewportDataSource,
        stateTransition: NavigationCameraStateTransition =
            MapboxNavigationCameraStateTransition(mapboxMap, cameraPlugin),
    ) : this(
        mapboxMap,
        cameraPlugin,
        viewportDataSource,
        stateTransition,
        UpdateFrameAnimatorsOptions.Builder().build(),
    )

    @ExperimentalPreviewMapboxNavigationAPI
    constructor(
        mapboxMap: MapboxMap,
        cameraPlugin: CameraAnimationsPlugin,
        viewportDataSource: ViewportDataSource,
        stateTransition: NavigationCameraStateTransition =
            MapboxNavigationCameraStateTransition(mapboxMap, cameraPlugin),
        updateFrameAnimatorsOptions: UpdateFrameAnimatorsOptions,
    ) : this(
        mapboxMap,
        cameraPlugin,
        viewportDataSource,
        getAnimatorsCreator(
            mapboxMap,
            cameraPlugin,
            stateTransition,
            updateFrameAnimatorsOptions,
        ),
    )

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    internal constructor(
        mapboxMap: MapboxMap,
        cameraPlugin: CameraAnimationsPlugin,
        viewportDataSource: ViewportDataSource,
        transitionProvider: NavigationCameraStateTransitionProvider,
        simplifiedUpdateFrameTransitionProvider: SimplifiedUpdateFrameTransitionProvider,
    ) : this(
        mapboxMap,
        cameraPlugin,
        viewportDataSource,
        SimplifiedFrameAnimatorsCreator(
            cameraPlugin,
            mapboxMap,
            transitionProvider,
            simplifiedUpdateFrameTransitionProvider,
        ),
    )

    companion object {

        private const val LOG_CATEGORY = "NavigationCamera"
        private const val LOG_CAMERA_STATE_SAMPLING_PERIOD_MILLIS = 1000L
        private const val APPLY_MAP_HINT_WHILE_IN_OVERVIEW_SAMPLING_PERIOD_MILLIS = 1000L
        private const val LOG_INVALID_MAP_SAMPLING_PERIOD_MILLIS = 1000L

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

        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun getAnimatorsCreator(
            mapboxMap: MapboxMap,
            cameraPlugin: CameraAnimationsPlugin,
            stateTransition: NavigationCameraStateTransition,
            updateFrameAnimatorsOptions: UpdateFrameAnimatorsOptions,
        ): AnimatorsCreator {
            return when (updateFrameAnimatorsOptions.useSimplifiedAnimatorsDependency) {
                true -> {
                    SimplifiedFrameAnimatorsCreator(
                        cameraPlugin,
                        mapboxMap,
                        NavigationCameraStateTransitionWrapper(stateTransition),
                        DefaultSimplifiedUpdateFrameTransitionProvider(cameraPlugin),
                    )
                }

                false -> {
                    FullFrameAnimatorsCreator(stateTransition, cameraPlugin, mapboxMap)
                }
            }
        }
    }

    private var runningAnimation: MapboxAnimatorSet? = null
    private val transitionEndListeners = CopyOnWriteArraySet<TransitionEndListener>()
    private var frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT

    private var lastCameraHintTime = 0L
    private var prevFollowingCameraForOverviewAnimationHint: CameraOptions? = null

    private val navigationCameraStateChangedObservers =
        CopyOnWriteArraySet<NavigationCameraStateChangedObserver>()

    private val navigationCameraStateChangedObserversInternal =
        CopyOnWriteArraySet<NavigationCameraStateChangedObserverInternal>()

    private var currentStateTransitionListener: NavigationCameraTransitionListener? = null

    /**
     * Returns current [NavigationCameraState].
     *
     * This is a projection of [stateInternal] used purely for public notifications: the internal
     * points-overview states collapse onto [NavigationCameraState.OVERVIEW] / [NavigationCameraState.TRANSITION_TO_OVERVIEW]. Core logic must
     * drive [stateInternal] and never assign this directly.
     * @see registerNavigationCameraStateChangeObserver
     */
    var state: NavigationCameraState = NavigationCameraState.IDLE
        private set(value) {
            if (value != field) {
                field = value
                navigationCameraStateChangedObservers.forEach {
                    it.onNavigationCameraStateChanged(value)
                }
            }
        }

    /**
     * The state that drives the camera state machine. Public [state] is derived from it; assigning
     * a value here updates [state] (de-duplicated) whenever the projected public state changes.
     */
    internal var stateInternal: NavigationCameraStateInternal = IDLE
        private set(value) {
            if (value != field) {
                field = value
                // the debugger distinguishes route and points overview, so it follows the internal
                // state which doesn't collapse them onto a single overview state
                updateDebugger()
                state = value.toNavigationCameraState()
                navigationCameraStateChangedObserversInternal.forEach {
                    it.onNavigationCameraStateChanged(value)
                }
            }
        }

    /**
     * Set a [MapboxNavigationViewportDataSourceDebugger].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    var debugger: MapboxNavigationViewportDataSourceDebugger? = null
        set(value) {
            field = value
            updateDebugger()
        }

    private var lastCameraStateLogTime = 0L
    private var lastInvalidMapLogTime = 0L
    private val sourceUpdateObserver =
        ViewportDataSourceUpdateObserver { viewportData ->
            if (!isMapValid()) return@ViewportDataSourceUpdateObserver
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastCameraStateLogTime >= LOG_CAMERA_STATE_SAMPLING_PERIOD_MILLIS) {
                logI(
                    "Current camera state = ${mapboxMap.cameraState}, " +
                        "viewport update = $viewportData",
                    LOG_CATEGORY,
                )
                lastCameraStateLogTime = currentTime
            }
            updateFrame(viewportData, instant = false)
        }

    init {
        viewportDataSource.registerUpdateObserver(sourceUpdateObserver)
    }

    /**
     * Executes a transition to [NavigationCameraState.FOLLOWING] state. When started, goes to [NavigationCameraState.TRANSITION_TO_FOLLOWING]
     * and to the final [NavigationCameraState.FOLLOWING] when ended.
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptionsBlock options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptionsBlock options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [NavigationCameraState.FOLLOWING] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param transitionEndListener invoked when transition ends.
     */
    @JvmOverloads
    fun requestNavigationCameraToFollowing(
        stateTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        frameTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        transitionEndListener: TransitionEndListener? = null,
    ) {
        requestNavigationCameraToFollowing(
            NavigationCameraTransitionOptions.Builder().apply(stateTransitionOptionsBlock).build(),
            NavigationCameraTransitionOptions.Builder().apply(frameTransitionOptionsBlock).build(),
            transitionEndListener,
        )
    }

    /**
     * Executes a transition to [NavigationCameraState.FOLLOWING] state. When started, goes to [NavigationCameraState.TRANSITION_TO_FOLLOWING]
     * and to the final [NavigationCameraState.FOLLOWING] when ended.
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptions options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptions options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [NavigationCameraState.FOLLOWING] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param transitionEndListener invoked when transition ends.
     */
    @JvmOverloads
    fun requestNavigationCameraToFollowing(
        stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
        frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
        transitionEndListener: TransitionEndListener? = null,
    ) {
        when (stateInternal) {
            FOLLOWING -> {
                transitionEndListener?.onTransitionEnd(isCanceled = false)
            }

            IDLE,
            TRANSITION_TO_ROUTE_OVERVIEW,
            ROUTE_OVERVIEW,
            TRANSITION_TO_POINTS_OVERVIEW,
            POINTS_OVERVIEW,
            TRANSITION_TO_FOLLOWING,
            -> {
                startAnimation(
                    getFollowingTransition(
                        stateTransitionOptions,
                        frameTransitionOptions,
                    ),
                    instant = false,
                    transitionEndListener,
                )
            }
        }
    }

    private fun getFollowingTransition(
        stateTransitionOptions: NavigationCameraTransitionOptions,
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        val cameraForFollowing = viewportDataSource.getViewportData().cameraForFollowing
        return animatorsCreator.transitionToFollowing(
            cameraForFollowing,
            stateTransitionOptions,
        ).apply {
            addListener(
                createTransitionListener(
                    TRANSITION_TO_FOLLOWING,
                    FOLLOWING,
                    frameTransitionOptions,
                ).also {
                    this@NavigationCamera.currentStateTransitionListener = it
                },
            )
        }
    }

    /**
     * Executes a transition to [NavigationCameraState.OVERVIEW] state. When started, goes to [NavigationCameraState.TRANSITION_TO_OVERVIEW]
     * and to the final [NavigationCameraState.OVERVIEW] when ended.
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptionsBlock options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptionsBlock options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [NavigationCameraState.OVERVIEW] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param transitionEndListener invoked when transition ends.
     */
    @JvmOverloads
    fun requestNavigationCameraToOverview(
        stateTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        frameTransitionOptionsBlock: ((NavigationCameraTransitionOptions.Builder).() -> Unit),
        transitionEndListener: TransitionEndListener? = null,
    ) {
        requestNavigationCameraToOverview(
            NavigationCameraTransitionOptions.Builder().apply(stateTransitionOptionsBlock).build(),
            NavigationCameraTransitionOptions.Builder().apply(frameTransitionOptionsBlock).build(),
            transitionEndListener,
        )
    }

    /**
     * Executes a transition to [NavigationCameraState.OVERVIEW] state. When started, goes to [NavigationCameraState.TRANSITION_TO_OVERVIEW]
     * and to the final [NavigationCameraState.OVERVIEW] when ended.
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptions options that impact the transition animation from the current state to the requested state.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 3500 millis.
     * @param frameTransitionOptions options that impact the transition animations between viewport frames in the selected state.
     * This refers to camera transition on each [ViewportDataSource] update when [NavigationCameraState.OVERVIEW] is engaged.
     * Defaults to [NavigationCameraTransitionOptions.maxDuration] equal to 1000 millis.
     * @param transitionEndListener invoked when transition ends.
     */
    @JvmOverloads
    fun requestNavigationCameraToOverview(
        stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
        frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
        transitionEndListener: TransitionEndListener? = null,
    ) {
        when (stateInternal) {
            ROUTE_OVERVIEW -> {
                transitionEndListener?.onTransitionEnd(isCanceled = false)
            }

            IDLE,
            TRANSITION_TO_FOLLOWING,
            FOLLOWING,
            TRANSITION_TO_POINTS_OVERVIEW,
            POINTS_OVERVIEW,
            TRANSITION_TO_ROUTE_OVERVIEW,
            -> {
                startAnimation(
                    getOverviewTransition(
                        stateTransitionOptions,
                        frameTransitionOptions,
                    ),
                    instant = false,
                    transitionEndListener,
                )
            }
        }
    }

    private fun getOverviewTransition(
        stateTransitionOptions: NavigationCameraTransitionOptions,
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        val cameraForOverview = viewportDataSource.getViewportData().cameraForOverview
        return animatorsCreator.transitionToRouteOverview(
            cameraForOverview,
            stateTransitionOptions,
        ).apply {
            addListener(
                createTransitionListener(
                    TRANSITION_TO_ROUTE_OVERVIEW,
                    ROUTE_OVERVIEW,
                    frameTransitionOptions,
                ).also {
                    this@NavigationCamera.currentStateTransitionListener = it
                },
            )
        }
    }

    /**
     * Executes a transition to the points overview, framing an arbitrary set of points
     * (see [MapboxNavigationViewportDataSource]) rather than the route geometry.
     *
     * Publicly this is reported as [NavigationCameraState.OVERVIEW] via [state] and the state observers.
     *
     * The target camera position is obtained with [ViewportDataSource.getViewportData].
     *
     * @param stateTransitionOptions options that impact the transition animation from the current state to the requested state.
     * @param frameTransitionOptions options that impact the transition animations between viewport frames in the selected state.
     * @param transitionEndListener invoked when transition ends.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    internal fun requestNavigationCameraToPointsOverview(
        stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
        frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
        transitionEndListener: TransitionEndListener? = null,
    ) {
        when (stateInternal) {
            POINTS_OVERVIEW -> {
                transitionEndListener?.onTransitionEnd(isCanceled = false)
            }

            IDLE,
            TRANSITION_TO_FOLLOWING,
            FOLLOWING,
            TRANSITION_TO_ROUTE_OVERVIEW,
            ROUTE_OVERVIEW,
            TRANSITION_TO_POINTS_OVERVIEW,
            -> {
                startAnimation(
                    getPointsOverviewTransition(
                        stateTransitionOptions,
                        frameTransitionOptions,
                    ),
                    instant = false,
                    transitionEndListener,
                )
            }
        }
    }

    private fun getPointsOverviewTransition(
        stateTransitionOptions: NavigationCameraTransitionOptions,
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        val cameraForPointsOverview =
            viewportDataSource.getViewportData().cameraForPointsOverview
        return animatorsCreator.transitionToPointsOverview(
            cameraForPointsOverview,
            stateTransitionOptions,
        ).apply {
            addListener(
                createTransitionListener(
                    TRANSITION_TO_POINTS_OVERVIEW,
                    POINTS_OVERVIEW,
                    frameTransitionOptions,
                ).also {
                    this@NavigationCamera.currentStateTransitionListener = it
                },
            )
        }
    }

    /**
     * Immediately goes to [NavigationCameraState.IDLE] state canceling all ongoing transitions.
     */
    fun requestNavigationCameraToIdle() {
        if (stateInternal != IDLE) {
            cancelAnimation()
            setIdleProperties()
        }
    }

    /**
     * If the [state] is [NavigationCameraState.FOLLOWING] or [NavigationCameraState.OVERVIEW],
     * performs an immediate camera transition (a jump, with animation duration equal to `0`)
     * based on the latest data obtained with [ViewportDataSource.getViewportData].
     */
    fun resetFrame() {
        val viewportData = viewportDataSource.getViewportData()
        updateFrame(viewportData, instant = true)
    }

    private fun updateFrame(viewportData: ViewportData, instant: Boolean) {
        if (!isMapValid()) return
        when (stateInternal) {
            FOLLOWING -> {
                startAnimation(
                    animatorsCreator.updateFrameForFollowing(
                        viewportData.cameraForFollowing,
                        frameTransitionOptions,
                    ).apply {
                        addAnimationEndListener(createFrameListener())
                    },
                    instant,
                )
            }

            ROUTE_OVERVIEW -> {
                setCameraAnimationHintWhileInOverview(viewportData.cameraForFollowing)
                startAnimation(
                    animatorsCreator.updateFrameForOverview(
                        viewportData.cameraForOverview,
                        frameTransitionOptions,
                    ).apply {
                        addAnimationEndListener(createFrameListener())
                    },
                    instant,
                )
            }

            POINTS_OVERVIEW -> {
                setCameraAnimationHintWhileInOverview(viewportData.cameraForFollowing)
                startAnimation(
                    animatorsCreator.updateFrameForOverview(
                        viewportData.cameraForPointsOverview,
                        frameTransitionOptions,
                    ).apply {
                        addAnimationEndListener(createFrameListener())
                    },
                    instant,
                )
            }

            IDLE,
            TRANSITION_TO_FOLLOWING,
            TRANSITION_TO_ROUTE_OVERVIEW,
            TRANSITION_TO_POINTS_OVERVIEW,
            -> {
                // no impl
            }
        }
    }

    /**
     * Pre-loading map tiles around the puck while in the overview to unload CPU when
     * animation to following mode starts
     */
    @OptIn(MapboxExperimental::class)
    private fun setCameraAnimationHintWhileInOverview(cameraForFollowing: CameraOptions) {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastCameraHintTime >=
            APPLY_MAP_HINT_WHILE_IN_OVERVIEW_SAMPLING_PERIOD_MILLIS &&
            prevFollowingCameraForOverviewAnimationHint != cameraForFollowing
        ) {
            lastCameraHintTime = currentTime
            prevFollowingCameraForOverviewAnimationHint = cameraForFollowing
            logI(
                "Applying camera hint for the target camera state = $cameraForFollowing",
                LOG_CATEGORY,
            )
            mapboxMap.setCameraAnimationHint(
                CameraAnimationHint.Builder().stages(
                    listOf(
                        CameraAnimationHintStage.Builder()
                            .camera(cameraForFollowing)
                            .progress(1)
                            .build(),
                    ),
                ).build(),
            )
        }
    }

    /**
     * Registers [NavigationCameraStateChangedObserver].
     */
    fun registerNavigationCameraStateChangeObserver(
        navigationCameraStateChangedObserver: NavigationCameraStateChangedObserver,
    ) {
        navigationCameraStateChangedObservers.add(navigationCameraStateChangedObserver)
        navigationCameraStateChangedObserver.onNavigationCameraStateChanged(state)
    }

    /**
     * Unregisters [NavigationCameraStateChangedObserver].
     */
    fun unregisterNavigationCameraStateChangeObserver(
        navigationCameraStateChangedObserver: NavigationCameraStateChangedObserver,
    ) {
        navigationCameraStateChangedObservers.remove(navigationCameraStateChangedObserver)
    }

    /**
     * Registers [NavigationCameraStateChangedObserverInternal] that is notified with the
     * fine-grained [stateInternal] (keeping the points-overview distinction).
     */
    internal fun registerNavigationCameraStateChangeObserverInternal(
        observer: NavigationCameraStateChangedObserverInternal,
    ) {
        navigationCameraStateChangedObserversInternal.add(observer)
        observer.onNavigationCameraStateChanged(stateInternal)
    }

    /**
     * Unregisters [NavigationCameraStateChangedObserverInternal].
     */
    internal fun unregisterNavigationCameraStateChangeObserverInternal(
        observer: NavigationCameraStateChangedObserverInternal,
    ) {
        navigationCameraStateChangedObserversInternal.remove(observer)
    }

    private fun setIdleProperties() {
        this@NavigationCamera.frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT
        stateInternal = IDLE
    }

    private fun cancelAnimation() {
        runningAnimation?.cancel()
        runningAnimation = null
    }

    private fun startAnimation(
        animatorSet: MapboxAnimatorSet,
        instant: Boolean,
        transitionEndListener: TransitionEndListener? = null,
    ) {
        cancelAnimation()
        if (transitionEndListener != null) {
            transitionEndListeners.add(transitionEndListener)
        }
        if (instant) {
            animatorSet.makeInstant()
        }

        // workaround for https://github.com/mapbox/mapbox-maps-android/issues/277
        cameraPlugin.anchor = null

        animatorSet.start()
        runningAnimation = animatorSet
    }

    private fun finishAnimation(animatorSet: MapboxAnimatorSet) {
        if (runningAnimation === animatorSet) {
            runningAnimation = null
        }
    }

    private fun createTransitionListener(
        progressState: NavigationCameraStateInternal,
        finalState: NavigationCameraStateInternal,
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ) = NavigationCameraTransitionListener(
        progressState,
        finalState,
        frameTransitionOptions,
    )

    private fun createFrameListener() = object : MapboxAnimatorSetListener {

        override fun onAnimationStart(animation: MapboxAnimatorSet) {
            // no impl
        }

        override fun onAnimationEnd(animation: MapboxAnimatorSet) {
            finishAnimation(animation)
        }

        override fun onAnimationCancel(animation: MapboxAnimatorSet) {
            // no impl
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebugger() {
        debugger?.cameraState = stateInternal
    }

    /**
     * Returns whether the attached [mapboxMap] is still valid. This guard skips those calls and
     * logs a single throttled warning.
     */
    private fun isMapValid(): Boolean {
        if (mapboxMap.isValid()) return true
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastInvalidMapLogTime >= LOG_INVALID_MAP_SAMPLING_PERIOD_MILLIS) {
            logW(
                "MapboxMap is invalid, skipping camera update. The attached MapView is likely " +
                    "destroyed; detach the NavigationCamera or stop feeding it viewport updates.",
                LOG_CATEGORY,
            )
            lastInvalidMapLogTime = currentTime
        }
        return false
    }

    /**
     * Updates following frame transition options on the fly.
     * Note that these options will be reset on the next requestToFollowing invocation.
     */
    internal fun updateFollowingFrameTransitionOptions(
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ) {
        when (stateInternal) {
            FOLLOWING -> {
                this.frameTransitionOptions = frameTransitionOptions
            }

            TRANSITION_TO_FOLLOWING -> {
                currentStateTransitionListener?.frameTransitionOptions = frameTransitionOptions
            }

            else -> {
                // no-op
            }
        }
    }

    /**
     * Updates overview frame transition options on the fly.
     * Note that these options will be reset on the next requestToOverview invocation.
     */
    internal fun updateOverviewFrameTransitionOptions(
        frameTransitionOptions: NavigationCameraTransitionOptions,
    ) {
        when (stateInternal) {
            ROUTE_OVERVIEW -> this.frameTransitionOptions = frameTransitionOptions

            TRANSITION_TO_ROUTE_OVERVIEW -> {
                currentStateTransitionListener?.frameTransitionOptions = frameTransitionOptions
            }

            else -> {
                // no-op
            }
        }
    }

    private inner class NavigationCameraTransitionListener(
        private val progressState: NavigationCameraStateInternal,
        private val finalState: NavigationCameraStateInternal,
        var frameTransitionOptions: NavigationCameraTransitionOptions,
    ) : MapboxAnimatorSetListener {
        private var isCanceled = false

        override fun onAnimationStart(animation: MapboxAnimatorSet) {
            this@NavigationCamera.frameTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT
            stateInternal = progressState
        }

        override fun onAnimationEnd(animation: MapboxAnimatorSet) {
            if (!isCanceled) {
                this@NavigationCamera.frameTransitionOptions = frameTransitionOptions
                stateInternal = finalState
            }

            this@NavigationCamera.currentStateTransitionListener = null

            finishAnimation(animation)
            // Custom transitionEndListener might synchronously start another transition.
            // In this case we risk running into a race condition where the new transitionEndListeners
            // will be cleared at the next line before its animation is ended.
            // To avoid this, we first clear the existing listeners and only then notify them.
            val listeners = transitionEndListeners.toSet()
            transitionEndListeners.clear()
            listeners.forEach { it.onTransitionEnd(isCanceled) }
            updateFrame(viewportDataSource.getViewportData(), instant = false)
        }

        override fun onAnimationCancel(animation: MapboxAnimatorSet) {
            isCanceled = true
        }
    }
}
