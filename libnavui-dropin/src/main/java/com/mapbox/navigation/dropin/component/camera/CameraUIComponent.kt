package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
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

    private val _cameraUpdatesInhibited = MutableStateFlow(false)
    val cameraUpdatesInhibited: Flow<Boolean> = _cameraUpdatesInhibited

    // todo simplify passing flows from view model
    /*val cameraUpdatesInhibited: Flow<Boolean> = viewModel.state.map {
        it.hasUpdatesInhibited()
    }*/

    private val viewportDataSource = MapboxNavigationViewportDataSource(
        mapView.getMapboxMap()
    )
    private val navigationCamera = NavigationCamera(
        mapView.getMapboxMap(),
        mapView.camera,
        viewportDataSource
    )
    private val debugger = MapboxNavigationViewportDataSourceDebugger(mapView.context, mapView)

    var first = true

    init {
        debugger.apply {
            viewportDataSource.debugger = this
            navigationCamera.debugger = this
            // change to enable debug
            // todo expose as a setting
            enabled = true
        }
        navigationCamera.registerNavigationCameraStateChangeObserver {
            // todo find a better way to ignore the initial state of the underlying camera after configuration change
            if (!first) {
                if (it == NavigationCameraState.IDLE) {
                    val action = flowOf(CameraAction.OnTrackingBroken)
                    viewModel.consumeAction(action)
                }
            }
            first = false
            lifecycleOwner.lifecycleScope.launch {
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

        viewModel.consumeAction(flowOf(CameraAction.OnCameraInitialized))

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
        val action = flowOf(CameraAction.OnRecenterButtonClicked)
        viewModel.consumeAction(action)
    }

    override fun onOverviewButtonClicked() {
        val action = flowOf(CameraAction.OnOverviewButtonClicked)
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
                viewportDataSource.followingPadding = state.followingPadding
                viewportDataSource.overviewPadding = state.overviewPadding
                viewportDataSource.evaluate()

                val instantTransitionOptions = NavigationCameraTransitionOptions.Builder()
                    .maxDuration(0L)
                    .build()
                when (state.targetCameraState) {
                    TargetCameraState.IDLE ->
                        navigationCamera.requestNavigationCameraToIdle()
                    TargetCameraState.FOLLOWING -> {
                        if (state.resetFrame) {
                            navigationCamera.requestNavigationCameraToFollowing(
                                stateTransitionOptions = instantTransitionOptions
                            )
                        } else {
                            navigationCamera.requestNavigationCameraToFollowing()
                        }
                    }
                    TargetCameraState.OVERVIEW -> {
                        if (state.resetFrame) {
                            navigationCamera.requestNavigationCameraToOverview(
                                stateTransitionOptions = instantTransitionOptions
                            )
                        } else {
                            navigationCamera.requestNavigationCameraToOverview()
                        }
                    }
                }

                if (state.resetFrame) {
                    navigationCamera.resetFrame()
                }

                _cameraUpdatesInhibited.emit(state.hasUpdatesInhibited())
            }
        }
    }
}
