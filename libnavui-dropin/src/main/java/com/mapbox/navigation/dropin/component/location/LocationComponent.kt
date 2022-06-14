package com.mapbox.navigation.dropin.component.location

import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.R
import com.mapbox.navigation.ui.app.internal.controller.LocationStateController
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class LocationComponent(
    private val mapView: MapView,
    private val locationViewModel: LocationStateController,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            locationViewModel.firstLocation()
            mapView.getMapboxMap().getStyle {
                mapView.location.apply {
                    setLocationProvider(locationViewModel.navigationLocationProvider)
                    locationPuck = LocationPuck2D(
                        bearingImage = ContextCompat.getDrawable(
                            mapView.context,
                            R.drawable.mapbox_navigation_puck_icon
                        )
                    )
                    enabled = true
                }
            }
        }
    }
}
