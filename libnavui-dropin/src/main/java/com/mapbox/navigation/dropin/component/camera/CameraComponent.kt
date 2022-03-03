package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.extensions.flowNavigationCameraState
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class CameraComponent constructor(
    private val mapView: MapView,
    private val locationViewModel: LocationViewModel,
    private val cameraViewModel: CameraViewModel,
) : UIComponent() {

    private var isLocationInitialized = false
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            cameraViewModel.invoke(CameraAction.ToIdle)
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            // no op
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        viewportDataSource = MapboxNavigationViewportDataSource(mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
        mapView.gestures.addOnMoveListener(onMoveListener)
        coroutineScope.launch {
            cameraViewModel.state.collect {
                when (it.cameraAnimation) {
                    is CameraAnimate.EaseTo -> {
                        mapView.camera.easeTo(it.cameraOptions)
                    }
                    is CameraAnimate.FlyTo -> {
                        mapView.camera.flyTo(it.cameraOptions)
                    }
                    is CameraAnimate.SetTo -> {
                        mapView.getMapboxMap().setCamera(it.cameraOptions)
                    }
                }
                it.location?.let { enhancedLocation ->
                    updateCamera(it.cameraMode, enhancedLocation)
                }
                when (it.cameraTransition) {
                    is CameraTransition.ToFollowing -> {
                        navigationCamera.requestNavigationCameraToFollowing()
                    }
                    is CameraTransition.ToOverview -> {
                        navigationCamera.requestNavigationCameraToOverview()
                    }
                    is CameraTransition.ToIdle -> {
                        navigationCamera.requestNavigationCameraToIdle()
                    }
                }
            }
        }
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                viewportDataSource.onRouteProgressChanged(routeProgress)
                viewportDataSource.evaluate()
            }
        }
        coroutineScope.launch {
            mapboxNavigation.flowRoutesUpdated().collect { result ->
                if (result.routes.isEmpty()) {
                    viewportDataSource.clearRouteData()
                } else {
                    viewportDataSource.onRouteChanged(result.routes.first())
                }
                viewportDataSource.evaluate()
            }
        }
        coroutineScope.launch {
            navigationCamera.flowNavigationCameraState().collect {
                when (it) {
                    NavigationCameraState.TRANSITION_TO_FOLLOWING,
                    NavigationCameraState.FOLLOWING -> {
                        cameraViewModel.invoke(CameraAction.ToFollowing)
                    }
                    NavigationCameraState.TRANSITION_TO_OVERVIEW,
                    NavigationCameraState.OVERVIEW -> {
                        cameraViewModel.invoke(CameraAction.ToOverview)
                    }
                    NavigationCameraState.IDLE -> {
                        cameraViewModel.invoke(CameraAction.ToIdle)
                    }
                }
            }
        }
        coroutineScope.launch {
            locationViewModel.state.filterNotNull().collect {
                cameraViewModel.invoke(CameraAction.UpdateLocation(it))
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapView.gestures.removeOnMoveListener(onMoveListener)
        super.onDetached(mapboxNavigation)
    }

    private fun updateCamera(cameraMode: CameraMode, enhancedLocation: Location) {
        viewportDataSource.onLocationChanged(enhancedLocation)
        viewportDataSource.evaluate()
        if (!isLocationInitialized) {
            isLocationInitialized = true
            when (cameraMode) {
                CameraMode.IDLE -> {
                    navigationCamera.requestNavigationCameraToIdle()
                }
                CameraMode.FOLLOWING -> {
                    navigationCamera.requestNavigationCameraToFollowing()
                }
                CameraMode.OVERVIEW -> {
                    navigationCamera.requestNavigationCameraToOverview()
                }
            }
        }
    }
}
