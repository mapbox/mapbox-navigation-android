package com.mapbox.navigation.dropin.component.infopanel.routepreview

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.launch

class InfoPanelRoutePreviewComponent(view: View) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
        }
    }
}
