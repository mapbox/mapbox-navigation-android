package com.mapbox.navigation.qa_test_app.lifecycle

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.common.location.compat.LocationEngineCallback
import com.mapbox.common.location.compat.LocationEngineResult
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInLocationViewModel : ViewModel() {
    val navigationLocationProvider = NavigationLocationProvider()
    val locationLiveData = MutableLiveData<Location>()

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints
            )
            locationLiveData.value = locationMatcherResult.enhancedLocation
        }
    }

    private val navigationObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            val locationEngine = mapboxNavigation.navigationOptions.locationEngine
            locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    result.lastLocation?.let {
                        navigationLocationProvider.changePosition(it, emptyList())
                        locationLiveData.value = it
                    }
                }
                override fun onFailure(exception: Exception) {
                    Log.e("kyle_fail", "what is it", exception)
                }
            })

            mapboxNavigation.registerLocationObserver(locationObserver)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
        }
    }

    init {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onCleared() {
        MapboxNavigationApp.unregisterObserver(navigationObserver)
    }
}
