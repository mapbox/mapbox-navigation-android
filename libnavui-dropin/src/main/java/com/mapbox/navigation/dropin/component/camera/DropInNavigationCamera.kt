package com.mapbox.navigation.dropin.component.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.location.Location
import android.view.ViewTreeObserver
import androidx.lifecycle.asFlow
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationCamera(
    private val cameraState: DropInCameraState,
    private val mapView: MapView,
) : UIComponent() {
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private var isLocationInitialized = false
    private val edgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val visibleArea = Rect()
        val isVisible = mapView.getGlobalVisibleRect(visibleArea)
        check(isVisible) { "Make sure the map is visible" }
        visibleAreaChanged(visibleArea, edgeInsets)
    }

    private val triggerIdleCameraOnMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            if (cameraState.triggerIdleCameraOnMoveListener) {
                cameraState.cameraMode.value = DropInCameraMode.IDLE
            }
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            // No op
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val locationStateManager = MapboxNavigationApp.getObserver(LocationBehavior::class)
        super.onAttached(mapboxNavigation)
        mapView.gestures.addOnMoveListener(triggerIdleCameraOnMoveListener)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )

        check(mapView.viewTreeObserver.isAlive) { "Make sure the map is alive" }
        mapView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        coroutineScope.launch {
            locationStateManager.locationLiveData.asFlow().collect {
                // TODO we don't really want to do this. isLocationInitialized is also attempting
                //    to create the correct initialization experience.
                updateCamera(cameraState.cameraMode(), it)
            }
        }

        cameraState.cameraOptions.value?.let { builder ->
            mapView.getMapboxMap().setCamera(builder.build())
        }

        coroutineScope.launch {
            cameraState.cameraMode.asFlow().collect { cameraMode ->
                if (!isLocationInitialized) return@collect
                when (cameraMode) {
                    DropInCameraMode.IDLE ->
                        navigationCamera.requestNavigationCameraToIdle()
                    DropInCameraMode.FOLLOWING ->
                        navigationCamera.requestNavigationCameraToFollowing()
                    DropInCameraMode.OVERVIEW ->
                        navigationCamera.requestNavigationCameraToOverview()
                    null -> { /** no op */ }
                }
            }
        }

        mapboxNavigation.apply {
            registerRoutesObserver(routeObserver)
            registerRouteProgressObserver(routeProgressObserver)
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMoveListener(triggerIdleCameraOnMoveListener)
        mapboxNavigation.apply {
            unregisterRoutesObserver(routeObserver)
            unregisterRouteProgressObserver(routeProgressObserver)
        }
        isLocationInitialized = false
    }

    private fun visibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        viewportDataSource.overviewPadding = EdgeInsets(
            edgeInsets.top + OVERVIEW_PADDING,
            edgeInsets.left + OVERVIEW_PADDING,
            edgeInsets.bottom + OVERVIEW_PADDING,
            edgeInsets.right + OVERVIEW_PADDING
        )

        val visibleHeight = visibleArea.bottom - visibleArea.top
        val followingBottomPadding = visibleHeight * BOTTOM_FOLLOWING_PERCENTAGE
        viewportDataSource.followingPadding = EdgeInsets(
            edgeInsets.top,
            edgeInsets.left,
            edgeInsets.bottom + followingBottomPadding,
            edgeInsets.right
        )

        viewportDataSource.evaluate()
    }

    private fun updateCamera(cameraMode: DropInCameraMode, enhancedLocation: Location) {
        // Initialize the camera at the current location. The next location will
        // transition into the following or overview mode.
        viewportDataSource.onLocationChanged(enhancedLocation)
        viewportDataSource.evaluate()
        if (!isLocationInitialized) {
            isLocationInitialized = true
            val instantTransition = NavigationCameraTransitionOptions.Builder()
                .maxDuration(0)
                .build()
            when (cameraMode) {
                DropInCameraMode.IDLE -> {
                    // TODO When changing orientation, the state can be idle but haven't found
                    //   a way to save the previous state. This is not quite correct because the
                    //   viewportDataSource depends on the Map and has to be refreshed outside of
                    //   the view model.
                    cameraState.cameraOptions.value?.let { builder ->
                        val cameraOptions = builder.center(
                            Point.fromLngLat(enhancedLocation.longitude, enhancedLocation.latitude)
                        ).build()
                        mapView.getMapboxMap().setCamera(cameraOptions)
                    }
                    navigationCamera.requestNavigationCameraToIdle()
                }
                DropInCameraMode.FOLLOWING -> navigationCamera.requestNavigationCameraToFollowing(
                    stateTransitionOptions = instantTransition
                )
                DropInCameraMode.OVERVIEW -> navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = instantTransition
                )
            }
        }
    }

    private val routeObserver = RoutesObserver { result ->
        if (result.routes.isEmpty()) {
            viewportDataSource.clearRouteData()
        } else {
            viewportDataSource.onRouteChanged(result.routes.first())
        }
        viewportDataSource.evaluate()
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
    }

    companion object {
        /**
         * While following the location puck, inset the bottom by 1/3 of the screen.
         */
        private const val BOTTOM_FOLLOWING_PERCENTAGE = 1.0 / 3.0

        /**
         * While overviewing a route, add padding to thew viewport.
         */
        private const val OVERVIEW_PADDING = 5
    }
}
