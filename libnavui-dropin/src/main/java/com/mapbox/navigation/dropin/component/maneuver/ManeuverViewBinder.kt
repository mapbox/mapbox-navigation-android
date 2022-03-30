package com.mapbox.navigation.dropin.component.maneuver

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.databinding.MapboxManeuverGuidanceLayoutBinding
import com.mapbox.navigation.dropin.lifecycle.reloadOnChange
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ManeuverViewBinder(
    private val loadedMapStyle: StateFlow<Style?>
) : UIBinder {
    override fun bind(value: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            value,
            R.layout.mapbox_maneuver_guidance_layout,
            value.context
        )
        TransitionManager.go(scene)
        val binding = MapboxManeuverGuidanceLayoutBinding.bind(value)

        return reloadOnChange(loadedMapStyle) {
            if (it != null) ManeuverComponent(binding.maneuverView, it)
            else null
        }
    }
}
