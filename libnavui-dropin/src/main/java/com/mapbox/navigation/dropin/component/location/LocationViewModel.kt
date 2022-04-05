package com.mapbox.navigation.dropin.component.location

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class LocationAction {
    data class Update(val result: LocationMatcherResult) : LocationAction()
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationViewModel : UIViewModel<LocationMatcherResult?, LocationAction>(null) {
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
        state: LocationMatcherResult?,
        action: LocationAction
    ): LocationMatcherResult? {
        return when (action) {
            is LocationAction.Update -> action.result
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect {
                navigationLocationProvider.changePosition(
                    location = it.enhancedLocation,
                    keyPoints = it.keyPoints,
                )
                invoke(LocationAction.Update(it))
            }
        }
    }

    private companion object {
        const val DELAY_FIRST_LOCATION_MS = 100L
    }
}
