package com.mapbox.navigation.dropin.component.location

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationComponent(
    private val mapView: MapView,
    private val locationViewModel: LocationViewModel
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
