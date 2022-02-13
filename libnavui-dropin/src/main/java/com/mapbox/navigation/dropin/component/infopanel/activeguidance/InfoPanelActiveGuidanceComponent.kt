package com.mapbox.navigation.dropin.component.infopanel.activeguidance

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.launch

class InfoPanelActiveGuidanceComponent(view: View) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
        }
    }
}
