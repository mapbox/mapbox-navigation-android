package com.mapbox.navigation.qa_test_app.lifecycle

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.R

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInLocationComponent(
    private val mapView: MapView,
    private val dropInLocationViewModel: DropInLocationViewModel
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        mapView.location.apply {
            setLocationProvider(dropInLocationViewModel.navigationLocationProvider)
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
