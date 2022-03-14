package com.mapbox.navigation.dropin.component.maneuver

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.databinding.MapboxManeuverGuidanceLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ManeuverViewBinder : UIBinder {
    override fun bind(value: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            value,
            R.layout.mapbox_maneuver_guidance_layout,
            value.context
        )
        TransitionManager.go(scene)
        val binding = MapboxManeuverGuidanceLayoutBinding.bind(value)

        return ManeuverComponent(binding.maneuverView)
    }
}
