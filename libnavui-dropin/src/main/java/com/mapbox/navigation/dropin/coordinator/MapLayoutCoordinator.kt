package com.mapbox.navigation.dropin.coordinator

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.transition.Scene
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.map.ActiveGuidanceMapBinder
import com.mapbox.navigation.dropin.binder.map.FreeDriveMapBinder
import com.mapbox.navigation.dropin.binder.map.RoutePreviewMapBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.dropin.databinding.MapboxMapviewLayoutBinding
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class MapLayoutCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    private val binding: DropInNavigationViewBinding
) : UICoordinator<ViewGroup>(binding.mapViewLayout) {

    private val viewGroup = binding.mapViewLayout
    private val navigationStateViewModel = navigationViewContext.viewModel.navigationStateViewModel
    private val mapStyleLoader = navigationViewContext.mapStyleLoader
    private var reloadStyleJob: Job? = null

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapStyleLoader.mapboxMap = null
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return navigationViewContext.mapView
            .map { mapViewOverride ->
                if (mapViewOverride == null) {
                    Scene.getSceneForLayout(
                        viewGroup,
                        R.layout.mapbox_mapview_layout,
                        viewGroup.context,
                    ).enter()

                    val binding = MapboxMapviewLayoutBinding.bind(viewGroup)
                    initDefaultMap(binding.mapView.getMapboxMap())
                    binding.mapView
                } else {
                    initCustomMap(mapViewOverride.getMapboxMap())
                    viewGroup.removeAllViews()
                    viewGroup.addView(mapViewOverride)
                    mapViewOverride
                }
            }
            .combine(navigationStateViewModel.state) { mapView, navigationState ->
                MapBinder(
                    navigationViewContext,
                    binding,
                    binding.root.resources,
                    navigationState,
                    mapView
                )
            }
    }

    private fun initDefaultMap(mapboxMap: MapboxMap) {
        mapStyleLoader.mapboxMap = mapboxMap
        mapStyleLoader.loadInitialStyle() // immediately load map style to avoid map flashing
        reloadStyleJob = coroutineScope.launch {
            mapStyleLoader.observeAndReloadNewStyles()
        }
    }

    private fun initCustomMap(mapboxMap: MapboxMap) {
        reloadStyleJob?.cancel()
        mapStyleLoader.mapboxMap = mapboxMap
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class MapBinder(
    private val navigationViewContext: DropInNavigationViewContext,
    private val binding: DropInNavigationViewBinding,
    private val resources: Resources,
    private val navigationState: NavigationState,
    private val mapView: MapView
) : Binder<ViewGroup> {

    private val paddingObserver = object : UIComponent() {
        private val vPadding = resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
        private val hPadding = resources
            .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()
        private val cameraViewModel = navigationViewContext.viewModel.cameraViewModel
        private val navigationStateViewModel =
            navigationViewContext.viewModel.navigationStateViewModel

        private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            cameraViewModel.invoke(CameraAction.UpdatePadding(getOverlayEdgeInsets()))
        }

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)
            binding.coordinatorLayout.addOnLayoutChangeListener(layoutListener)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            super.onDetached(mapboxNavigation)
            binding.coordinatorLayout.removeOnLayoutChangeListener(layoutListener)
        }

        private fun getOverlayEdgeInsets(): EdgeInsets {
            return when (navigationStateViewModel.state.value) {
                is NavigationState.DestinationPreview,
                is NavigationState.FreeDrive,
                is NavigationState.RoutePreview -> {
                    val bottom = vPadding + (mapView.height - binding.infoPanelLayout.top)
                    EdgeInsets(vPadding, hPadding, bottom, hPadding)
                }
                is NavigationState.ActiveNavigation,
                is NavigationState.Arrival -> {
                    val bottom = vPadding + (mapView.height - binding.roadNameLayout.top)
                    EdgeInsets(
                        vPadding + binding.guidanceLayout.height,
                        hPadding,
                        bottom,
                        hPadding
                    )
                }
            }
        }
    }

    init {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false
    }

    override fun bind(value: ViewGroup): MapboxNavigationObserver {
        val observers = when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview -> FreeDriveMapBinder(navigationViewContext)
                .bind(mapView)
            NavigationState.RoutePreview -> RoutePreviewMapBinder(navigationViewContext)
                .bind(mapView)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival -> ActiveGuidanceMapBinder(navigationViewContext)
                .bind(mapView)
        }
        return navigationListOf(
            paddingObserver,
            observers
        )
    }
}
