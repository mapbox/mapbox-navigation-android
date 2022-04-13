package com.mapbox.navigation.dropin.component.marker

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logW

@ExperimentalPreviewMapboxNavigationAPI
internal class RoutePreviewLongPressMapComponent(
    private val mapView: MapView,
    private val locationViewModel: LocationViewModel,
    private val routesViewModel: RoutesViewModel,
    private val destinationViewModel: DestinationViewModel,
) : UIComponent() {

    private var hapticFeedback: HapticFeedback? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        hapticFeedback =
            HapticFeedback.create(mapboxNavigation.navigationOptions.applicationContext)
        mapView.gestures.addOnMapLongClickListener(longClickListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(longClickListener)
        hapticFeedback = null
    }

    private val longClickListener = OnMapLongClickListener { point ->
        ifNonNull(locationViewModel.lastPoint) { lastPoint ->
            destinationViewModel.invoke(DestinationAction.SetDestination(Destination(point)))
            routesViewModel.invoke(RoutesAction.FetchPoints(listOf(lastPoint, point)))
            hapticFeedback?.tick()
        } ?: logW(TAG, "Current location is unknown so map long press does nothing")
        false
    }

    private companion object {
        private val TAG = this::class.java.simpleName
    }
}
