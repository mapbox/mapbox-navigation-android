package com.mapbox.navigation.dropin.component.roadlabel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.databinding.MapboxRoadNameLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoadNameViewBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_road_name_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxRoadNameLayoutBinding.bind(viewGroup)
        return RoadNameLabelComponent(binding.roadNameView)
    }
}
