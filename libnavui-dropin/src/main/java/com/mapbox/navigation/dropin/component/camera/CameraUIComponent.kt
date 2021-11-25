package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.recenter.OnRecenterButtonClickedListener
import com.mapbox.navigation.dropin.component.routeoverview.OnOverviewButtonClickedListener
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal interface CameraUIComponent : UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxCameraUIComponent(
    mapView: MapView,
    private val viewModel: CameraViewModel,
    private val lifecycleOwner: LifecycleOwner,
) : CameraUIComponent,
    LocationObserver,
    RoutesObserver,
    RouteProgressObserver,
    OnRecenterButtonClickedListener,
    OnOverviewButtonClickedListener {

    private val _navigationCameraState = MutableStateFlow(NavigationCameraState.IDLE)
    val navigationCameraState: Flow<NavigationCameraState> = _navigationCameraState

    private val viewportDataSource = MapboxNavigationViewportDataSource(
        mapView.getMapboxMap()
    )
    private val navigationCamera = NavigationCamera(
        mapView.getMapboxMap(),
        mapView.camera,
        viewportDataSource
    )
    private val debugger = MapboxNavigationViewportDataSourceDebugger(mapView.context, mapView)

    init {
        debugger.apply {
            viewportDataSource.debugger = this
            navigationCamera.debugger = this
            // change to enable debug
            // todo expose as a setting
            enabled = true
        }
        navigationCamera.registerNavigationCameraStateChangeObserver {
            if (it == NavigationCameraState.IDLE) {
                val action = flowOf(CameraAction.OnTrackingBroken)
                viewModel.consumeAction(action)
            }
            viewModel.viewModelScope.launch {
                _navigationCameraState.emit(it)
            }
        }
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationScaleGestureHandler(
                mapView.context,
                navigationCamera,
                mapView.getMapboxMap(),
                mapView.gestures,
                mapView.location,
                {
                    val action = flowOf(CameraAction.OnZoomGestureWhileTracking)
                    viewModel.consumeAction(action)
                    // todo show recenter button
                }
            ).apply { initialize() }
        )
        observeCameraState()
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        // no impl
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        val action = flowOf(CameraAction.UpdateRoute(result.routes))
        viewModel.consumeAction(action)
    }

    override fun onNewRawLocation(rawLocation: Location) {
        val action = flowOf(CameraAction.UpdateRawLocation(rawLocation))
        viewModel.consumeAction(action)
    }

    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        val action = flowOf(CameraAction.UpdateLocation(locationMatcherResult))
        viewModel.consumeAction(action)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val action = flowOf(CameraAction.UpdateRouteProgress(routeProgress))
        viewModel.consumeAction(action)
    }

    override fun onRecenterButtonClicked() {
        val action = flowOf(CameraAction.OnRecenterButtonCLicked)
        viewModel.consumeAction(action)
    }

    override fun onOverviewButtonClicked() {
        val action = flowOf(CameraAction.OnOverviewButtonCLicked)
        viewModel.consumeAction(action)
    }

    private fun observeCameraState() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                state.location?.let {
                    viewportDataSource.onLocationChanged(it)
                }
                state.route.let {
                    if (it != null) {
                        viewportDataSource.onRouteChanged(it)
                    } else {
                        viewportDataSource.clearRouteData()
                    }
                }
                state.routeProgress?.let {
                    viewportDataSource.onRouteProgressChanged(it)
                }
                viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed =
                    state.zoomUpdatesAllowed
                viewportDataSource.evaluate()

                when (state.targetCameraState) {
                    TargetCameraState.IDLE ->
                        navigationCamera.requestNavigationCameraToIdle()
                    TargetCameraState.FOLLOWING ->
                        navigationCamera.requestNavigationCameraToFollowing()
                    TargetCameraState.OVERVIEW ->
                        navigationCamera.requestNavigationCameraToOverview()
                }
            }
        }
    }
}
