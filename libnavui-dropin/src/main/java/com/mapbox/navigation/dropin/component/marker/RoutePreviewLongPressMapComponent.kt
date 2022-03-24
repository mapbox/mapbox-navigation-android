package com.mapbox.navigation.dropin.component.marker

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logW

internal class RoutePreviewLongPressMapComponent(
    private val mapView: MapView,
    private val locationViewModel: LocationViewModel,
    private val routesViewModel: RoutesViewModel,
    private val destinationViewModel: DestinationViewModel,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapView.gestures.addOnMapLongClickListener(longClickListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(longClickListener)
    }

    private val longClickListener = OnMapLongClickListener { point ->
        ifNonNull(locationViewModel.lastPoint) { lastPoint ->
            destinationViewModel.invoke(DestinationAction.SetDestination(Destination(point)))
            routesViewModel.invoke(RoutesAction.FetchPoints(listOf(lastPoint, point)))
        } ?: logW(TAG, "Current location is unknown so map long press does nothing")
        false
    }

    private companion object {
        private val TAG = this::class.java.simpleName
    }
}
