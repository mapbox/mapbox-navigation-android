package com.mapbox.navigation.dropin.controller

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.location.LocationAction
import com.mapbox.navigation.dropin.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.delay

@ExperimentalPreviewMapboxNavigationAPI
internal class LocationStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

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

    override fun process(state: State, action: Action): State {
        if (action is LocationAction) {
            return state.copy(location = processLocationAction(state.location, action))
        }
        return state
    }

    private fun processLocationAction(
        state: LocationMatcherResult?,
        action: LocationAction
    ): LocationMatcherResult? {
        return when (action) {
            is LocationAction.Update -> action.result
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult().observe {
            navigationLocationProvider.changePosition(
                location = it.enhancedLocation,
                keyPoints = it.keyPoints,
            )
            store.dispatch(LocationAction.Update(it))
        }
    }

    private companion object {
        const val DELAY_FIRST_LOCATION_MS = 100L
    }
}
