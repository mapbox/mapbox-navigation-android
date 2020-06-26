package com.mapbox.navigation.core

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
     */
    @JvmStatic
    fun create(navigationOptions: NavigationOptions): MapboxNavigation {
        synchronized(MapboxNavigationProvider::class.java) {
            mapboxNavigation?.onDestroy()
            mapboxNavigation = MapboxNavigation(
                navigationOptions
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
