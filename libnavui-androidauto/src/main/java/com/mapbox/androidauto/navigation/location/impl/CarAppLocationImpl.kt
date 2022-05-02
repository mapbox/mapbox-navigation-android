package com.mapbox.androidauto.navigation.location.impl

import android.location.Location
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@ExperimentalPreviewMapboxNavigationAPI
internal class CarAppLocationImpl : CarAppLocation {

    override val navigationLocationProvider = NavigationLocationProvider()

    private val locationObserver = object : LocationObserver {

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
        }

        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("CarAppLocationImpl onAttached")
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("CarAppLocationImpl onDetached")
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    override suspend fun validLocation(): Location? = withContext(Dispatchers.Unconfined) {
        var location: Location? = navigationLocationProvider.lastLocation
        while (isActive && location == null) {
            delay(DELAY_MILLISECONDS)
            location = navigationLocationProvider.lastLocation
        }
        logAndroidAuto("CarAppLocationImpl validLocation")
        return@withContext location
    }

    companion object {
        const val DELAY_MILLISECONDS = 100L
    }
}
