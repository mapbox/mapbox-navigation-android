package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

class ExampleProgressChangeListener(
    private val location: MutableLiveData<Location>,
    private val progress: MutableLiveData<RouteProgress>
) : RouteProgressObserver, LocationObserver {

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        this.progress.value = routeProgress
    }

    override fun onRawLocationChanged(rawLocation: Location) {
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location) {
        this.location.value = enhancedLocation
    }
}
