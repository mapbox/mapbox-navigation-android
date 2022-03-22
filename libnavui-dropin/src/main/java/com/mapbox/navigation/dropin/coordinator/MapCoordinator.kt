package com.mapbox.navigation.dropin.coordinator

import android.view.View
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.map.ActiveGuidanceMapBinder
import com.mapbox.navigation.dropin.binder.map.FreeDriveMapBinder
import com.mapbox.navigation.dropin.binder.map.RoutePreviewMapBinder
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for the map.
 * This will include camera, location puck, and route line.
 */
@OptIn(MapboxExperimental::class)
internal class MapCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    private val binding: DropInNavigationViewBinding
) : UICoordinator<MapView>(binding.mapView) {

    private val resources = binding.root.resources
    private val vPadding =
        resources.getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val hPadding =
        resources.getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()
    private val viewModel = navigationViewContext.viewModel.cameraViewModel
    private val navigationState get() = navigationViewContext.navigationState

    init {
        binding.mapView.compass.enabled = false
        binding.mapView.scalebar.enabled = false
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        binding.coordinatorLayout.addOnLayoutChangeListener(layoutListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        binding.coordinatorLayout.removeOnLayoutChangeListener(layoutListener)
    }

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        viewModel.invoke(CameraAction.UpdatePadding(getOverlayEdgeInsets()))
    }

    private fun getOverlayEdgeInsets(): EdgeInsets {
        return when (navigationState.value) {
            is NavigationState.Empty,
            is NavigationState.FreeDrive,
            is NavigationState.RoutePreview -> {
                val bottom = vPadding + (binding.mapView.height - binding.infoPanelLayout.top)
                EdgeInsets(vPadding, hPadding, bottom, hPadding)
            }
            is NavigationState.ActiveNavigation,
            is NavigationState.Arrival -> {
                val bottom = vPadding + (binding.mapView.height - binding.roadNameLayout.top)
                EdgeInsets(
                    vPadding + binding.guidanceLayout.height,
                    hPadding,
                    bottom,
                    hPadding
                )
            }
        }
    }

    // Temporarily flow to wire the map states
    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<MapView>> {
        return navigationViewContext.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.Empty,
                NavigationState.FreeDrive -> FreeDriveMapBinder(navigationViewContext)
                NavigationState.RoutePreview -> RoutePreviewMapBinder(navigationViewContext)
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> ActiveGuidanceMapBinder(navigationViewContext)
            }
        }
    }
}
