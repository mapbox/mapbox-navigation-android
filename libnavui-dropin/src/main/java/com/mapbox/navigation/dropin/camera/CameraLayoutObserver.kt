package com.mapbox.navigation.dropin.camera

import android.content.res.Configuration
import android.view.View
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

internal class CameraLayoutObserver(
    private val store: Store,
    private val mapView: View,
    private val binding: MapboxNavigationViewLayoutBinding,
) : UIComponent() {

    private val vPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val hPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()
    private val vPaddingLandscape = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_landscape_v).toDouble()
    private val hPaddingLandscape = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_landscape_h).toDouble()

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val edgeInsets = when (deviceOrientation()) {
            Configuration.ORIENTATION_LANDSCAPE -> getLandscapePadding()
            else -> getPortraitPadding()
        }
        store.dispatch(CameraAction.UpdatePadding(edgeInsets))
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        binding.coordinatorLayout.addOnLayoutChangeListener(layoutListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        binding.coordinatorLayout.removeOnLayoutChangeListener(layoutListener)
    }

    private fun getPortraitPadding(): EdgeInsets {
        val top = binding.guidanceLayout.height.toDouble()
        val bottom = mapView.height.toDouble() - binding.roadNameLayout.top.toDouble()
        return when (store.state.value.navigation) {
            is NavigationState.DestinationPreview,
            is NavigationState.FreeDrive,
            is NavigationState.RoutePreview -> {
                EdgeInsets(vPadding, hPadding, bottom, hPadding)
            }
            is NavigationState.ActiveNavigation,
            is NavigationState.Arrival -> {
                EdgeInsets(vPadding + top, hPadding, bottom, hPadding)
            }
        }
    }

    private fun getLandscapePadding(): EdgeInsets {
        val left = mapView.width.toDouble() - binding.infoPanelLayout.right.toDouble()
        val right = mapView.width.toDouble() - binding.actionListLayout.left.toDouble()
        val bottom = mapView.height.toDouble() - binding.roadNameLayout.top.toDouble()
        return when (store.state.value.navigation) {
            is NavigationState.FreeDrive -> {
                EdgeInsets(vPaddingLandscape, hPaddingLandscape, bottom, hPaddingLandscape)
            }
            is NavigationState.DestinationPreview,
            is NavigationState.RoutePreview,
            is NavigationState.ActiveNavigation,
            is NavigationState.Arrival -> {
                EdgeInsets(
                    vPaddingLandscape,
                    hPaddingLandscape + left,
                    bottom,
                    right - hPaddingLandscape
                )
            }
        }
    }

    private fun deviceOrientation() = binding.root.resources.configuration.orientation
}
