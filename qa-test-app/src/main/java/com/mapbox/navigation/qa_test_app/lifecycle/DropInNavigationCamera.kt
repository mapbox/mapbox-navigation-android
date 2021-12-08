package com.mapbox.navigation.qa_test_app.lifecycle

import android.annotation.SuppressLint
import android.graphics.Rect
import android.location.Location
import android.view.ViewTreeObserver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions

private const val DEFAULT_INITIAL_ZOOM = 15.0

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationCamera(
    val mapView: MapView,
    private val cameraMode: DropInCameraMode,
    private val dropInLocationViewModel: DropInLocationViewModel,
    private val initialCameraOptions: CameraOptions? = CameraOptions.Builder()
        .zoom(DEFAULT_INITIAL_ZOOM)
        .build(),
) : DefaultLifecycleObserver {
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

    override fun onCreate(owner: LifecycleOwner) {
        initialCameraOptions?.let { mapView.getMapboxMap().setCamera(it) }

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

        dropInLocationViewModel.locationLiveData.observe(owner) {
            updateCamera(it)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mapView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun onResume(owner: LifecycleOwner) {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onPause(owner: LifecycleOwner) {
        MapboxNavigationApp.unregisterObserver(navigationObserver)
    }

    private val navigationObserver = object : MapboxNavigationObserver {
        @SuppressLint("MissingPermission")
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.apply {
                registerRoutesObserver(routeObserver)
                registerRouteProgressObserver(routeProgressObserver)
            }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.apply {
                unregisterRoutesObserver(routeObserver)
                unregisterRouteProgressObserver(routeProgressObserver)
            }
            isLocationInitialized = false
        }
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

    private fun updateCamera(enhancedLocation: Location) {
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
                DropInCameraMode.IDLE -> navigationCamera.requestNavigationCameraToIdle()
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

    private companion object {
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
