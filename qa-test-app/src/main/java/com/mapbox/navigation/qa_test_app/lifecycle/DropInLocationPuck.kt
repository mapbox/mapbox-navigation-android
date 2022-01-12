package com.mapbox.navigation.qa_test_app.lifecycle

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInLocationViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInLocationPuck(
    private val locationViewModel: DropInLocationViewModel,
    private val mapView: MapView
) : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapView.getMapboxMap().getStyle {
            mapView.location.apply {
                setLocationProvider(locationViewModel.navigationLocationProvider)
                enabled = true
                locationPuck = LocationPuck2D(
                    bearingImage = ContextCompat.getDrawable(
                        mapView.context,
                        R.drawable.mapbox_navigation_puck_icon
                    )
                )
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        // not needed
    }
}
