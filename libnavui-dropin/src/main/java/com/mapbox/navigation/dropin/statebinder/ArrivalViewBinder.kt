package com.mapbox.navigation.dropin.statebinder

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ArrivalViewBinder(
    val navigationViewContext: DropInNavigationViewContext
) : DropInViewBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                TODO("Not yet implemented")
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                TODO("Not yet implemented")
            }
        }
    }
}
