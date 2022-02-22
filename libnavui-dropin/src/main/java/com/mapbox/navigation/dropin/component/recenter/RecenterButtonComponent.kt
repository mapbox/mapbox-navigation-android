package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxExtendableButton

@ExperimentalPreviewMapboxNavigationAPI
internal class RecenterButtonComponent(
    private val recenterButton: MapboxExtendableButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val behaviour = MapboxNavigationApp.getObserver(RecenterButtonBehaviour::class)

        recenterButton.setOnClickListener {
            behaviour.onButtonClick()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        recenterButton.setOnClickListener(null)
    }
}
