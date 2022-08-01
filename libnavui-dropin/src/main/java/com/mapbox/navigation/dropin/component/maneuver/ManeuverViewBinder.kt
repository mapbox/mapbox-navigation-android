package com.mapbox.navigation.dropin.component.maneuver

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxManeuverGuidanceLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponent
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.internal.extensions.getUserId
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ManeuverViewBinder(
    private val loadedMapStyle: StateFlow<Style?>,
    private val maneuverViewOptions: StateFlow<ManeuverViewOptions>,
) : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_maneuver_guidance_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxManeuverGuidanceLayoutBinding.bind(viewGroup)

        return reloadOnChange(
            loadedMapStyle,
            maneuverViewOptions
        ) { mapStyle, options ->
            if (mapStyle != null) {
                ManeuverComponent(
                    maneuverView = binding.maneuverView,
                    userId = mapStyle.getUserId(),
                    styleId = mapStyle.getStyleId(),
                    options = options
                )
            } else {
                null
            }
        }
    }
}
