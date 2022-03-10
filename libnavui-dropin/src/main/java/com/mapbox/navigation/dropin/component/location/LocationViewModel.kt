package com.mapbox.navigation.dropin.component.location

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class LocationAction {
    data class Update(val location: Location) : LocationAction()
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationViewModel : UIViewModel<Location?, LocationAction>(null) {
    val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Accessor function to get the last [Point]
     */
    val lastPoint: Point?
        get() = navigationLocationProvider.lastLocation?.run {
            Point.fromLngLat(longitude, latitude)
        }

    /**
     * Suspend until a non-null location is available.
     */
    suspend fun firstLocation(): Location {
        var nonNullLocation: Location? = navigationLocationProvider.lastLocation
        while (nonNullLocation == null) {
            delay(DELAY_FIRST_LOCATION_MS)
            nonNullLocation = navigationLocationProvider.lastLocation
        }
        return nonNullLocation
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

        mainJobControl.scope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect { locationMatcherResult ->
                navigationLocationProvider.changePosition(
                    location = locationMatcherResult.enhancedLocation,
                    keyPoints = locationMatcherResult.keyPoints,
                )
                invoke(LocationAction.Update(locationMatcherResult.enhancedLocation))
            }
        }
    }

    private companion object {
        const val DELAY_FIRST_LOCATION_MS = 100L
    }
}
