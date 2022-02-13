package com.mapbox.navigation.dropin.component.infopanel.freedrive

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.launch

class InfoPanelFreeDriveComponent(view: View) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
        }
    }
}
