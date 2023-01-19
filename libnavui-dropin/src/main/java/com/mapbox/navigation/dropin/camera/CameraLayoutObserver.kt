package com.mapbox.navigation.dropin.camera

import android.content.res.Configuration
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

internal class CameraLayoutObserver(
    private val context: NavigationViewContext,
    private val mapView: View,
    private val binding: MapboxNavigationViewLayoutBinding,
) : UIComponent() {

    private val vPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_v).toDouble()
    private val hPadding = binding.root.resources
        .getDimensionPixelSize(R.dimen.mapbox_camera_overview_padding_h).toDouble()

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateCameraPadding()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        binding.coordinatorLayout.addOnLayoutChangeListener(layoutListener)
        context.behavior.infoPanelBehavior.bottomSheetState.observe { updateCameraPadding() }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        binding.coordinatorLayout.removeOnLayoutChangeListener(layoutListener)
    }

    private fun updateCameraPadding() {
        val edgeInsets = when (deviceOrientation()) {
            Configuration.ORIENTATION_LANDSCAPE -> getLandscapePadding()
            else -> getPortraitPadding()
        }
        context.store.dispatch(CameraAction.UpdatePadding(edgeInsets))
    }

    private fun getPortraitPadding(): EdgeInsets {
        val top = binding.guidanceLayout.bottom
        val bottom = mapView.height - binding.roadNameLayout.top
        return EdgeInsets(vPadding + top, hPadding, vPadding + bottom, hPadding)
    }

    private fun getLandscapePadding(): EdgeInsets {
        val bottomSheetState = context.behavior.infoPanelBehavior.bottomSheetState.value
        val isBottomSheetVisible =
            bottomSheetState != null && bottomSheetState != BottomSheetBehavior.STATE_HIDDEN
        val bottomSheetWidth = if (isBottomSheetVisible) binding.infoPanelLayout.right else 0
        val bottom = mapView.height - binding.roadNameLayout.top
        return EdgeInsets(vPadding, hPadding + bottomSheetWidth, vPadding + bottom, hPadding)
    }

    private fun deviceOrientation() = binding.root.resources.configuration.orientation
}
