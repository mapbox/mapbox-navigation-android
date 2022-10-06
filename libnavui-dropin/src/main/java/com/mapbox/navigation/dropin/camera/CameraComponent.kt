package com.mapbox.navigation.dropin.camera

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.toCameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.toTargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.internal.extensions.flowNavigationCameraState
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraComponent constructor(
    private val context: NavigationViewContext,
    private val mapView: MapView,
    private val viewportDataSource: MapboxNavigationViewportDataSource =
        MapboxNavigationViewportDataSource(
            mapboxMap = mapView.getMapboxMap()
        ),
    private val navigationCamera: NavigationCamera =
        NavigationCamera(
            mapboxMap = mapView.getMapboxMap(),
            cameraPlugin = mapView.camera,
            viewportDataSource = viewportDataSource
        ),
    private val debugger: MapboxNavigationViewportDataSourceDebugger =
        MapboxNavigationViewportDataSourceDebugger(
            context = mapView.context,
            mapView = mapView,
            layerAbove = "road-label"
        )
) : UIComponent() {
    private val store = context.store

    private var isCameraInitialized = false
    private val gesturesHandler = NavigationBasicGesturesHandler(navigationCamera)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        context.options.showCameraDebugInfo.observe { show ->
            if (show) {
                debugger.enabled = true
                navigationCamera.debugger = debugger
                viewportDataSource.debugger = debugger
            } else {
                debugger.enabled = false
                navigationCamera.debugger = null
                viewportDataSource.debugger = null
            }
        }

        mapView.camera.addCameraAnimationsLifecycleListener(gesturesHandler)

        store.select { it.camera.cameraPadding }.observe {
            viewportDataSource.overviewPadding = it
            viewportDataSource.followingPadding = it
            viewportDataSource.evaluate()
        }

        restoreCameraState()
        controlCameraFrameOverrides()
        syncNavigationCameraState()
        updateCameraLocation()

        onRouteProgressUpdates(mapboxNavigation)
        onRouteUpdates(mapboxNavigation)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        saveCameraState()
        mapView.camera.removeCameraAnimationsLifecycleListener(gesturesHandler)
    }

    private fun saveCameraState() {
        val map = mapView.getMapboxMap()
        store.dispatch(CameraAction.SaveMapState(map.cameraState))
        logD("saveCameraState $map; ${map.cameraState}", "CameraComponent")
    }

    private fun restoreCameraState() {
        val state = store.state.value.camera.mapCameraState
        val map = mapView.getMapboxMap()
        logD("restoreCameraState $map; $state", "CameraComponent")
        if (state != null) map.setCamera(state.toCameraOptions())
    }

    private fun controlCameraFrameOverrides() {
        viewportDataSource.overviewPitchPropertyOverride(OVERVIEW_PITCH_OVERRIDE)
        coroutineScope.launch {
            store.select { it.navigation }.collect {
                when (it) {
                    is NavigationState.FreeDrive -> {
                        viewportDataSource.followingZoomPropertyOverride(FOLLOWING_ZOOM_OVERRIDE)
                        viewportDataSource.overviewZoomPropertyOverride(OVERVIEW_ZOOM_OVERRIDE)
                    }
                    else -> {
                        viewportDataSource.followingZoomPropertyOverride(value = null)
                        viewportDataSource.overviewZoomPropertyOverride(value = null)
                    }
                }
                viewportDataSource.evaluate()
            }
        }
    }

    private fun syncNavigationCameraState() {
        // using immediate dispatcher causes nested store updates
        navigationCamera.flowNavigationCameraState().observe(Dispatchers.Main) {
            store.dispatch(SetCameraMode(it.toTargetCameraMode()))
        }

        store.select { it.camera.cameraMode }.observe(Dispatchers.Main) {
            val currentMode = navigationCamera.state.toTargetCameraMode()
            if (isCameraInitialized && it != currentMode) {
                when (it) {
                    is TargetCameraMode.Idle ->
                        navigationCamera.requestNavigationCameraToIdle()
                    is TargetCameraMode.Overview ->
                        navigationCamera.requestNavigationCameraToOverview()
                    is TargetCameraMode.Following ->
                        navigationCamera.requestNavigationCameraToFollowing()
                }
            }
        }
    }

    private fun updateCameraLocation() {
        coroutineScope.launch {
            combine(
                store.select { it.location?.enhancedLocation },
                store.select { it.navigation }
            ) { location, navigationState ->
                location?.let {
                    viewportDataSource.onLocationChanged(it)
                    viewportDataSource.evaluate()
                    if (!isCameraInitialized) {
                        when (navigationState) {
                            NavigationState.ActiveNavigation,
                            NavigationState.Arrival -> {
                                navigationCamera.requestNavigationCameraToFollowing(
                                    stateTransitionOptions = NavigationCameraTransitionOptions
                                        .Builder()
                                        .maxDuration(0) // instant transition
                                        .build()
                                )
                                store.dispatch(SetCameraMode(TargetCameraMode.Following))
                            }
                            else -> {
                                navigationCamera.requestNavigationCameraToOverview(
                                    stateTransitionOptions = NavigationCameraTransitionOptions
                                        .Builder()
                                        .maxDuration(0) // instant transition
                                        .build()
                                )
                                store.dispatch(SetCameraMode(TargetCameraMode.Overview))
                            }
                        }
                    }
                    isCameraInitialized = true
                }
            }.collect()
        }
    }

    private fun onRouteProgressUpdates(mapboxNavigation: MapboxNavigation) {
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                viewportDataSource.onRouteProgressChanged(routeProgress)
                viewportDataSource.evaluate()
            }
        }
    }

    private fun onRouteUpdates(mapboxNavigation: MapboxNavigation) {
        val routesFlow = combine(
            store.select { it.previewRoutes },
            mapboxNavigation.flowRoutesUpdated()
                .map { it.navigationRoutes }
                .stateIn(
                    coroutineScope,
                    SharingStarted.WhileSubscribed(),
                    mapboxNavigation.getNavigationRoutes(),
                ),
        ) { previewRoutes, navigationRoutes ->
            when {
                navigationRoutes.isNotEmpty() -> navigationRoutes
                previewRoutes is RoutePreviewState.Ready -> previewRoutes.routes
                else -> emptyList()
            }
        }
        routesFlow.observe { routes ->
            if (routes.isEmpty()) {
                viewportDataSource.clearRouteData()
                viewportDataSource.evaluate()
            } else {
                viewportDataSource.onRouteChanged(routes.first())
                viewportDataSource.evaluate()
            }
        }
        // using immediate dispatcher causes nested store updates
        coroutineScope.launch(Dispatchers.Main) {
            store.select { it.navigation }.collectLatest { navigationState ->
                when (navigationState) {
                    NavigationState.FreeDrive -> {
                        routesFlow.firstOrNull { it.isEmpty() } ?: return@collectLatest
                        store.dispatch(SetCameraMode(TargetCameraMode.Overview))
                    }
                    NavigationState.DestinationPreview -> {
                        store.dispatch(SetCameraMode(TargetCameraMode.Idle))
                    }
                    NavigationState.RoutePreview -> {
                        store.dispatch(SetCameraMode(TargetCameraMode.Overview))
                    }
                    NavigationState.ActiveNavigation, NavigationState.Arrival -> {
                        store.dispatch(SetCameraMode(TargetCameraMode.Following))
                    }
                }
            }
        }
    }

    companion object {
        private const val OVERVIEW_ZOOM_OVERRIDE = 16.5
        private const val OVERVIEW_PITCH_OVERRIDE = 0.0
        private const val FOLLOWING_ZOOM_OVERRIDE = 16.5
    }
}
