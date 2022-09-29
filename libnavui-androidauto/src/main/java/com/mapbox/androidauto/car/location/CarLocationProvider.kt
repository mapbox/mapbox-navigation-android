package com.mapbox.androidauto.car.location

import android.location.Location
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Location provider for the car.
 */
class CarLocationProvider internal constructor() : LocationProvider, MapboxNavigationObserver {

    private val delegate = NavigationLocationProvider()

    /**
     * Suspends until a non-null location is available. Returns null if the coroutine scope
     * becomes inactive.
     */
    suspend fun validLocation(): Location? = withContext(Dispatchers.Unconfined) {
        var location: Location? = delegate.lastLocation
        while (isActive && location == null) {
            delay(DELAY_MILLISECONDS)
            location = delegate.lastLocation
        }
        return@withContext location
    }

    /**
     * Returns the last cached target location value.
     */
    val lastLocation: Location?
        get() = delegate.lastLocation

    /**
     * Returns the last cached key points value.
     *
     * For precise puck's position use the [OnIndicatorPositionChangedListener].
     */
    val lastKeyPoints: List<Location>
        get() = delegate.lastKeyPoints

    /**
     * @see [LocationProvider.registerLocationConsumer]
     */
    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        delegate.registerLocationConsumer(locationConsumer)
    }

    /**
     * @see [LocationProvider.unRegisterLocationConsumer]
     */
    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        delegate.unRegisterLocationConsumer(locationConsumer)
    }

    private val locationObserver = object : LocationObserver {

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            delegate.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
        }

        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    companion object {
        private const val DELAY_MILLISECONDS = 100L

        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getInstance(): CarLocationProvider = MapboxNavigationApp
            .getObservers(CarLocationProvider::class).firstOrNull()
            ?: (CarLocationProvider().also { MapboxNavigationApp.registerObserver(it) })
    }
}
