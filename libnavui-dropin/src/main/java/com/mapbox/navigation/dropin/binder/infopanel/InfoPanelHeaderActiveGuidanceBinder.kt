package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderActiveGuidanceLayoutBinding
import com.mapbox.navigation.dropin.internal.endNavigationButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import kotlinx.coroutines.flow.map

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderActiveGuidanceBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_header_active_guidance_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelHeaderActiveGuidanceLayoutBinding.bind(viewGroup)

        return context.run {
            navigationListOf(
                tripProgressComponent(binding.tripProgressLayout),
                endNavigationButtonComponent(binding.endNavigation)
            )
        }
    }

    private fun tripProgressComponent(
        tripProgressLayout: ViewGroup
    ): MapboxNavigationObserver {
        val binderFlow = context.uiBinders.infoPanelTripProgressBinder.map {
            it ?: InfoPanelTripProgressBinder(context)
        }
        return reloadOnChange(binderFlow) { it.bind(tripProgressLayout) }
    }
}
