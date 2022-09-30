package com.mapbox.navigation.qa_test_app.lifecycle.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.logE

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInLocationViewModel : ViewModel() {

    val navigationLocationProvider = NavigationLocationProvider()

    private val _locationLiveData = MutableLiveData<Location>()
    val locationLiveData: LiveData<Location> = _locationLiveData

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints
            )
            _locationLiveData.value = locationMatcherResult.enhancedLocation
        }
    }

    private val navigationObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            val locationEngine = mapboxNavigation.navigationOptions.locationEngine
            locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    result.lastLocation?.let {
                        navigationLocationProvider.changePosition(it, emptyList())
                        _locationLiveData.value = it
                    }
                }
                override fun onFailure(exception: Exception) {
                    logE(
                        "Failed to get immediate location exception=$exception",
                        "DropInLocationViewModel"
                    )
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
