package com.mapbox.navigation.core

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.options.NavigationOptions

/**
 * Singleton responsible for ensuring there is only one MapboxNavigation instance.
 */
object MapboxNavigationProvider {
    @Volatile
    private var mapboxNavigation: MapboxNavigation? = null

    /**
     * Create MapboxNavigation with provided options.
     * Should be called before [retrieve]
     *
     * @param navigationOptions
     * @param locationEngine
     * @param locationEngineRequest
     */
    @JvmOverloads
    @JvmStatic
    fun create(
        navigationOptions: NavigationOptions,
        locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(navigationOptions.applicationContext),
        locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(1000L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
    ): MapboxNavigation {
        synchronized(MapboxNavigationProvider::class.java) {
            mapboxNavigation?.onDestroy()
            mapboxNavigation = MapboxNavigation(
                navigationOptions,
                locationEngine,
                locationEngineRequest
            )

            return mapboxNavigation!!
        }
    }

    /**
     * Retrieve MapboxNavigation instance.
     * Should be called after [create]
     */
    @JvmStatic
    fun retrieve(): MapboxNavigation {
        var instance = mapboxNavigation
        if (instance == null) {
            synchronized(MapboxNavigationProvider::class.java) {
                instance = mapboxNavigation
                if (instance == null) {
                    throw RuntimeException("Need to create MapboxNavigation before using it.")
                }
            }
        }

        return instance!!
    }
}
