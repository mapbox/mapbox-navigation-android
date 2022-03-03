package com.mapbox.navigation.dropin.component.map

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.UICommandDispatcher
import com.mapbox.navigation.dropin.lifecycle.UICommand
import com.mapbox.navigation.dropin.lifecycle.UIComponent

class MapComponent(
    val mapView: MapView,
    val dispatcher: UICommandDispatcher
): UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapView.gestures.addOnMapLongClickListener {
            dispatcher.dispatch(
                UICommand.MapCommand.OnMapLongClicked(
                    point = it,
                    map = mapView.getMapboxMap(),
                    padding = 0f
                )
            )
            false
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
    }
}
