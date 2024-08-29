package com.mapbox.navigation.ui.androidauto.location

import com.mapbox.common.location.Location
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * Automatically attaches and detaches from [MapboxNavigationApp] to provide map matched locations.
 */
class CarLocationProvider private constructor() : MapboxNavigationObserver, LocationProvider {

    private val navigationLocationProvider = NavigationLocationProvider()
    private val mutableLocation = MutableSharedFlow<Location>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val locationObserver = object : LocationObserver {
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            logD("YURY") { "onNewLocationMatcherResult: $locationMatcherResult" }
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            mutableLocation.tryEmit(locationMatcherResult.enhancedLocation)
        }

        override fun onNewRawLocation(rawLocation: Location) {
            // no-op
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        navigationLocationProvider.registerLocationConsumer(locationConsumer)
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        navigationLocationProvider.unRegisterLocationConsumer(locationConsumer)
    }

    /**
     * Immediately access the last location received.
     */
    fun lastLocation(): Location? = navigationLocationProvider.lastLocation

    /**
     * Wait until a non-null location is received. Improves results when the app is starting.
     */
    suspend fun validLocation(): Location = mutableLocation.first()

    companion object {
        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): CarLocationProvider = MapboxNavigationApp
            .getObservers(CarLocationProvider::class).firstOrNull()
            ?: CarLocationProvider().also { MapboxNavigationApp.registerObserver(it) }
    }
}
