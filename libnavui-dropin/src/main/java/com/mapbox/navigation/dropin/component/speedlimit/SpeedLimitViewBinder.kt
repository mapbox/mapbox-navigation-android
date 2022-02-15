package com.mapbox.navigation.dropin.component.speedlimit

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.databinding.MapboxSpeedLimitLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SpeedLimitViewBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_speed_limit_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxSpeedLimitLayoutBinding.bind(viewGroup)

        return SpeedLimitComponent(binding.speedLimitView)
    }
}
