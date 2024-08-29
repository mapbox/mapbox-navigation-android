package com.mapbox.navigation.ui.androidauto.navigation

import android.graphics.Rect
import androidx.annotation.UiThread
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.androidauto.DefaultMapboxCarMapGestureHandler
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpToPx
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.routes.NavigationCarRoutesProvider
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

private const val DEFAULT_INITIAL_ZOOM = 15.0

/**
 * Integrates the Android Auto [MapboxCarMapSurface] with the [NavigationCamera].
 *
 * @param initialCarCameraMode defines the initial [CarCameraMode]
 * @param alternativeCarCameraMode is an optional toggle [CarCameraMode]
 * @param carRoutesProvider provides routes that can adjust the camera view port
 * @param initialCameraOptions set camera options when the camera is attached
 */
@UiThread
class CarNavigationCamera(
    private val initialCarCameraMode: CarCameraMode,
    private val alternativeCarCameraMode: CarCameraMode?,
    private val carRoutesProvider: CarRoutesProvider = NavigationCarRoutesProvider(),
    private val initialCameraOptions: CameraOptions? = CameraOptions.Builder()
        .zoom(DEFAULT_INITIAL_ZOOM)
        .build(),
) : MapboxCarMapObserver {

    private var mapboxCarMapSurface: MapboxCarMapSurface? = null
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private var overviewPaddingPx by Delegates.notNull<Int>()
    private var followingPaddingPx by Delegates.notNull<Int>()
    private lateinit var coroutineScope: CoroutineScope

    private val _nextCameraMode = MutableStateFlow(alternativeCarCameraMode)

    /**
     * Allow you to observe what the next camera mode will be. The navigation camera lets you
     * toggle between the [initialCarCameraMode] and [alternativeCarCameraMode].
     *
     * For example, if your initial camera mode is [CarCameraMode.FOLLOWING] and the alternative
     * camera mode is [CarCameraMode.OVERVIEW]; if the current mode is overview, the
     * [nextCameraMode] is following.
     */
    val nextCameraMode: StateFlow<CarCameraMode?> = _nextCameraMode

    private var isLocationInitialized = false

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {
            // no-op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            // Initialize the camera at the current location. The next location will
            // transition into the following or overview mode.
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
            if (!isLocationInitialized) {
                isLocationInitialized = true
                val instantTransition = NavigationCameraTransitionOptions.Builder()
                    .maxDuration(0)
                    .build()
                when (initialCarCameraMode) {
                    CarCameraMode.IDLE -> navigationCamera.requestNavigationCameraToIdle()
                    CarCameraMode.FOLLOWING -> navigationCamera.requestNavigationCameraToFollowing(
                        stateTransitionOptions = instantTransition,
                    )

                    CarCameraMode.OVERVIEW -> navigationCamera.requestNavigationCameraToOverview(
                        stateTransitionOptions = instantTransition,
                    )
                }
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
    }

    private val navigationObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        }
    }

    /**
     * Connect the gesture handler to the map with [MapboxCarMap.setGestureHandler].
     */
    val gestureHandler = object : DefaultMapboxCarMapGestureHandler() {
        override fun onScroll(
            mapboxCarMapSurface: MapboxCarMapSurface,
            visibleCenter: ScreenCoordinate,
            distanceX: Float,
            distanceY: Float,
        ) {
            updateCameraMode(CarCameraMode.IDLE)
            super.onScroll(mapboxCarMapSurface, visibleCenter, distanceX, distanceY)
        }

        override fun onScale(
            mapboxCarMapSurface: MapboxCarMapSurface,
            focusX: Float,
            focusY: Float,
            scaleFactor: Float,
        ) {
            updateCameraMode(CarCameraMode.IDLE)
            val fromZoom = mapboxCarMapSurface.mapSurface.getMapboxMap().cameraState.zoom
            val toZoom = fromZoom - (1.0 - scaleFactor.toDouble())
            val inBounds = toZoom.coerceIn(MIN_ZOOM_OUT, MAX_ZOOM_IN) != toZoom
            if (!inBounds) {
                super.onScale(mapboxCarMapSurface, focusX, focusY, scaleFactor)
            }
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        coroutineScope = MainScope()
        this.mapboxCarMapSurface = mapboxCarMapSurface
        this.followingPaddingPx = mapboxCarMapSurface.carContext.dpToPx(
            FOLLOWING_OVERVIEW_PADDING_DP,
        )
        this.overviewPaddingPx = mapboxCarMapSurface.carContext.dpToPx(OVERVIEW_PADDING_DP)
        logAndroidAuto("CarNavigationCamera loaded $mapboxCarMapSurface")

        val mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap()
        initialCameraOptions?.let { mapboxMap.setCamera(it) }
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxCarMapSurface.mapSurface.getMapboxMap(),
        )
        navigationCamera = NavigationCamera(
            mapboxMap,
            mapboxCarMapSurface.mapSurface.camera,
            viewportDataSource,
        )

        MapboxNavigationApp.registerObserver(navigationObserver)

        coroutineScope.launch {
            carRoutesProvider.navigationRoutes.collect { routes ->
                if (routes.isEmpty()) {
                    viewportDataSource.clearRouteData()
                } else {
                    viewportDataSource.onRouteChanged(routes.first())
                }
                viewportDataSource.evaluate()
            }
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        super.onVisibleAreaChanged(visibleArea, edgeInsets)
        logAndroidAuto("CarNavigationCamera visibleAreaChanged $visibleArea $edgeInsets")

        viewportDataSource.overviewPadding = EdgeInsets(
            edgeInsets.top + overviewPaddingPx,
            edgeInsets.left + overviewPaddingPx,
            edgeInsets.bottom + overviewPaddingPx,
            edgeInsets.right + overviewPaddingPx,
        )

        val visibleHeight = visibleArea.bottom - visibleArea.top
        viewportDataSource.followingPadding = EdgeInsets(
            edgeInsets.top + followingPaddingPx,
            edgeInsets.left + followingPaddingPx,
            edgeInsets.bottom + visibleHeight * BOTTOM_FOLLOWING_FRACTION,
            edgeInsets.right + followingPaddingPx,
        )

        viewportDataSource.evaluate()
    }

    fun updateCameraMode(carCameraMode: CarCameraMode) {
        _nextCameraMode.value = if (carCameraMode != initialCarCameraMode) {
            initialCarCameraMode
        } else {
            alternativeCarCameraMode
        }
        when (carCameraMode) {
            CarCameraMode.IDLE -> navigationCamera.requestNavigationCameraToIdle()
            CarCameraMode.FOLLOWING -> navigationCamera.requestNavigationCameraToFollowing()
            CarCameraMode.OVERVIEW -> navigationCamera.requestNavigationCameraToOverview()
        }
    }

    /**
     * Function dedicated to zoom in map action buttons.
     */
    fun zoomInAction() = scaleEaseBy(ZOOM_ACTION_DELTA)

    /**
     * Function dedicated to zoom in map action buttons.
     */
    fun zoomOutAction() = scaleEaseBy(-ZOOM_ACTION_DELTA)

    /**
     * If true the camera may recalculate and update the zoom level. If false
     * the feature is disabled.
     */
    fun zoomUpdatesAllowed(allowed: Boolean) {
        viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = allowed
        viewportDataSource.options.overviewFrameOptions.zoomUpdatesAllowed = allowed
    }

    /**
     * Indicates whether the following camera is configured to recalculate and update the zoom level.
     */
    fun followingZoomUpdatesAllowed(): Boolean {
        return if (this::viewportDataSource.isInitialized) {
            viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed
        } else {
            true
        }
    }

    private fun scaleEaseBy(delta: Double) {
        val mapSurface = mapboxCarMapSurface?.mapSurface
        val fromZoom = mapSurface?.getMapboxMap()?.cameraState?.zoom ?: return
        val toZoom = (fromZoom + delta).coerceIn(MIN_ZOOM_OUT, MAX_ZOOM_IN)
        mapSurface.camera.easeTo(cameraOptions { zoom(toZoom) })
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarNavigationCamera detached $mapboxCarMapSurface")
        this.mapboxCarMapSurface = null
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        isLocationInitialized = false
        coroutineScope.cancel()
    }

    private companion object {
        /**
         * While following the location puck, inset the bottom by 1/3 of the screen.
         */
        private const val BOTTOM_FOLLOWING_FRACTION = 1.0 / 3.0

        /**
         * The following state will go into a zero-pitch state which requires padding for the left
         * top and right edges.
         */
        private const val FOLLOWING_OVERVIEW_PADDING_DP = 5

        /**
         * While overviewing a route, add padding to the viewport.
         */
        private const val OVERVIEW_PADDING_DP = 5

        /**
         * When zooming the camera by a delta, this is an estimated min-zoom.
         */
        private const val MIN_ZOOM_OUT = 6.0

        /**
         * When zooming the camera by a delta, this is an estimated max-zoom.
         */
        private const val MAX_ZOOM_IN = 20.0

        /**
         * Simple zoom delta to associate with the zoom action buttons.
         */
        private const val ZOOM_ACTION_DELTA = 0.5
    }
}
