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
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelActiveGuidanceComponent
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelArrivalComponent
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelActiveGuidanceLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ActiveGuidanceInfoPanelBinder(
    private val context: DropInNavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_active_guidance_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelActiveGuidanceLayoutBinding.bind(viewGroup)
        return navigationListOf(
            TripProgressComponent(binding.tripProgressView),
            InfoPanelActiveGuidanceComponent(
                context.stopActiveGuidanceUseCase(),
                binding.endNavigation
            ),
            InfoPanelArrivalComponent(
                context.viewModel,
                binding.arrivedText,
                binding.tripProgressView
            )
        )
    }
}
