package com.mapbox.navigation.dropin.component.location

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.LoggerProvider.logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationBehavior : MapboxNavigationObserver {

    val navigationLocationProvider = NavigationLocationProvider()

    private val _locationStateFlow = MutableStateFlow<Location?>(null)
    val locationStateFlow = _locationStateFlow.asStateFlow()

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints
            )
            _locationStateFlow.value = locationMatcherResult.enhancedLocation
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val locationEngine = mapboxNavigation.navigationOptions.locationEngine
        locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                result.lastLocation?.let {
                    navigationLocationProvider.changePosition(it, emptyList())
                    _locationStateFlow.value = it
                }
            }
            override fun onFailure(exception: Exception) {
                logger.e(
                    Tag("MbxDropInLocationObserver"),
                    Message("Failed to get immediate location"),
                    exception
                )
            }
        })

        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }
}
