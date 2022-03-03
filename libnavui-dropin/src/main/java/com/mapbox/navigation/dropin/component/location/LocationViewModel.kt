package com.mapbox.navigation.dropin.component.location

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.logE

sealed class LocationAction {
    data class Update(val location: Location) : LocationAction()
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationViewModel : UIViewModel<Location?, LocationAction>(null) {
    val navigationLocationProvider = NavigationLocationProvider()

    val lastPoint: Point?
        get() = navigationLocationProvider.lastLocation?.run {
            Point.fromLngLat(longitude, latitude)
        }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // We are not sending LocationAction.UpdateRaw here, because we are expecting to
            // receive locationMatcherResult
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                location = locationMatcherResult.enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )
            invoke(LocationAction.Update(locationMatcherResult.enhancedLocation))
        }
    }

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: Location?,
        action: LocationAction
    ): Location {
        return when (action) {
            is LocationAction.Update -> action.location
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val locationEngine = mapboxNavigation.navigationOptions.locationEngine
        locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                result.lastLocation?.let {
                    navigationLocationProvider.changePosition(it, emptyList())
                    invoke(LocationAction.Update(it))
                }
            }
            override fun onFailure(exception: Exception) {
                logE(
                    "MbxLocationViewModel",
                    "Failed to get immediate location exception=$exception"
                )
            }
        })

        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        super.onDetached(mapboxNavigation)
    }
}
