package com.mapbox.navigation.dropin.component.camera

import android.view.View
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.lifecycle.UIComponent

internal class CameraLayoutObserver(
    private val mapView: MapView,
    private val binding: MapboxNavigationViewLayoutBinding,
    private val cameraViewModel: CameraViewModel,
    private val navigationStateViewModel: NavigationStateViewModel
) : UIComponent() {

    private val vPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val hPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()

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
