package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelHeaderComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelHeaderBinder(
    private val context: DropInNavigationViewContext
) : UIBinder {

    private val tripProgressBinder get() = context.uiBinders.infoPanelTripProgressBinder.value

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_header_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxInfoPanelHeaderLayoutBinding.bind(viewGroup)
        return navigationListOf(
            InfoPanelHeaderComponent(
                binding,
                context.viewModel.navigationStateViewModel,
                context.viewModel.destinationViewModel,
                context.viewModel.locationViewModel,
                context.viewModel.routesViewModel,
            ),
            tripProgressBinder.bind(binding.tripProgressLayout),
        )
    }
}
