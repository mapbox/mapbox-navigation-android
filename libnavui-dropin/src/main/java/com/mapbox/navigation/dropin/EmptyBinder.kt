package com.mapbox.navigation.dropin

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

/**
 * The class represents transitioning an empty layout into the [ViewGroup] provided.
 */
@ExperimentalPreviewMapboxNavigationAPI
class EmptyBinder : UIBinder {
    /**
     * Triggered when this view binder instance is attached.
     *
     * @param viewGroup ViewGroup
     */
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        viewGroup.removeAllViews()
        return object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) = Unit
            override fun onDetached(mapboxNavigation: MapboxNavigation) = Unit
        }
    }
}
