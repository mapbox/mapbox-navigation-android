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
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelArrivalComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelArrivalLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ArrivalInfoPanelBinder(
    private val navigationViewContext: DropInNavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_arrival_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelArrivalLayoutBinding.bind(viewGroup)
        return navigationListOf(
            InfoPanelArrivalComponent(binding.root)
        )
    }
}
