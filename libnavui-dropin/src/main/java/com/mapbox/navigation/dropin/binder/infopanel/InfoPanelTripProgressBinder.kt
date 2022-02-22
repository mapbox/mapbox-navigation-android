package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelTripProgressLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelTripProgressBinder : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_trip_progress_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxInfoPanelTripProgressLayoutBinding.bind(viewGroup)
        return TripProgressComponent(binding.tripProgressView)
    }
}
