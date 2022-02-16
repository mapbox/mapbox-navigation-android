package com.mapbox.navigation.dropin.binder.action

import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EmptyActionBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_layout_drop_in_empty,
            viewGroup.context,
        )
        TransitionManager.go(scene, Slide(Gravity.RIGHT))
        return object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                // No op for empty view binder
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                // No op for empty view binder
            }
        }
    }
}
