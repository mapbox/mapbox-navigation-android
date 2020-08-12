package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.options.NavigationOptions

/**
 * Singleton responsible for ensuring there is only one MapboxNavigation instance.
 */
@UiThread
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
        mapboxNavigation?.onDestroy()
        mapboxNavigation = MapboxNavigation(
            navigationOptions
        )

        return mapboxNavigation!!
    }

    /**
     * Retrieve MapboxNavigation instance. Should be called after [create].
     *
     * @see [isCreated]
     */
    @JvmStatic
    fun retrieve(): MapboxNavigation {
        if (!isCreated()) {
            throw RuntimeException("Need to create MapboxNavigation before using it.")
        }

        return mapboxNavigation!!
    }

    /**
     * Destroy MapboxNavigation when your process/activity exits.
     */
    @JvmStatic
    fun destroy() {
        mapboxNavigation?.onDestroy()
        mapboxNavigation = null
    }

    /**
     * Check if MapboxNavigation is created.
     */
    @JvmStatic
    fun isCreated(): Boolean {
        return mapboxNavigation != null
    }
}
