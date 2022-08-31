package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderDestinationLayoutBinding
import com.mapbox.navigation.dropin.internal.poiNameComponent
import com.mapbox.navigation.dropin.internal.routePreviewButtonComponent
import com.mapbox.navigation.dropin.internal.startNavigationButtonComponent
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderDestinationPreviewBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_header_destination_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelHeaderDestinationLayoutBinding.bind(viewGroup)

        return context.run {
            navigationListOf(
                poiNameComponent(binding.poiName),
                routePreviewButtonComponent(binding.routePreview),
                startNavigationButtonComponent(binding.startNavigation)
            )
        }
    }
}
