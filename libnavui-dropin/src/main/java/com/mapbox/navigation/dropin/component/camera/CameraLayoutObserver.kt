package com.mapbox.navigation.dropin.component.camera

import android.content.res.Configuration
import android.view.View
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class CameraLayoutObserver(
    private val store: Store,
    private val mapView: MapView,
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
        val edgeInsets = when (binding.root.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                getLandscapePadding()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                getPortraitPadding()
            }
            else -> {
                getPortraitPadding()
            }
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
                EdgeInsets(vPadding, hPadding, vPadding.plus(bottom), hPadding)
            }
            is NavigationState.ActiveNavigation,
            is NavigationState.Arrival -> {
                EdgeInsets(vPadding.plus(top), hPadding, vPadding.plus(bottom), hPadding)
            }
        }
    }

    private fun getLandscapePadding(): EdgeInsets {
        val left = mapView.width.toDouble() - binding.infoPanelLayout.right.toDouble()
        val right = mapView.width.toDouble() - binding.actionListLayout.left.toDouble()
        val bottom = mapView.height.toDouble() - binding.roadNameLayout.top.toDouble()
        return when (store.state.value.navigation) {
            is NavigationState.FreeDrive -> {
                EdgeInsets(
                    vPaddingLandscape,
                    hPaddingLandscape,
                    vPaddingLandscape.plus(bottom),
                    hPaddingLandscape
                )
            }
            is NavigationState.DestinationPreview,
            is NavigationState.RoutePreview,
            is NavigationState.ActiveNavigation,
            is NavigationState.Arrival -> {
                EdgeInsets(
                    vPaddingLandscape,
                    hPaddingLandscape.plus(left),
                    vPaddingLandscape.plus(bottom),
                    hPaddingLandscape.plus(right)
                )
            }
        }
    }
}
