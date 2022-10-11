package com.mapbox.navigation.dropin.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderArrivalLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.arrivalTextComponent
import com.mapbox.navigation.dropin.internal.extensions.endNavigationButtonComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

internal class InfoPanelHeaderArrivalBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_header_arrival_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelHeaderArrivalLayoutBinding.bind(viewGroup)

        return context.run {
            navigationListOf(
                arrivalTextComponent(binding.arrivedTextContainer),
                endNavigationButtonComponent(binding.endNavigationButtonLayout)
            )
        }
    }
}
