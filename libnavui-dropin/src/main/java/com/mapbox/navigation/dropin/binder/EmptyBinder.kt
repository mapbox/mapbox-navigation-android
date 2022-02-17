package com.mapbox.navigation.dropin.binder

import android.transition.Scene
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R

/**
 * This does not have a transition specified. To have better control of
 * the transition, create a new binder with your specified transition.
 * You can reuse the R.layout.mapbox_layout_drop_in_empty
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EmptyBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_layout_drop_in_empty,
            viewGroup.context,
        ).enter()
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
