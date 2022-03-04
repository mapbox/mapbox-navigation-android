package com.mapbox.navigation.dropin.binder.map

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent

class MapLongClickRoutesComponent(
    private val mapView: MapView,
    private val locationViewModel: LocationViewModel,
    private val routesViewModel: RoutesViewModel,
) : UIComponent() {

    private val listener = OnMapLongClickListener { clickPoint ->
        locationViewModel.lastPoint?.let { lastPoint ->
            routesViewModel.invoke(
                RoutesAction.FetchPoints(
                    listOf(
                        Point.fromLngLat(
                            lastPoint.longitude(),
                            lastPoint.latitude(),
                        ),
                        clickPoint
                    )
                )
            )
        }
        false
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapView.gestures.addOnMapLongClickListener(listener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapView.gestures.removeOnMapLongClickListener(listener)
        super.onDetached(mapboxNavigation)
    }
}
