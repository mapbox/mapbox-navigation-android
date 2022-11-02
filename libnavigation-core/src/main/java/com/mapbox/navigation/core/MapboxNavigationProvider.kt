package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.internal.LegacyMapboxNavigationInstanceHolder

/**
 * Singleton responsible for ensuring there is only one MapboxNavigation instance.
 */
@UiThread
@Deprecated(
    message = "Use MapboxNavigationApp to attach MapboxNavigation to lifecycles."
)
object MapboxNavigationProvider {

    /**
     * Create MapboxNavigation with provided options.
     * Should be called before [retrieve]
     *
     * @param navigationOptions
     */
    @JvmStatic
    @Deprecated(
        message = "Set the navigation options with MapboxNavigationApp.setup"
    )
    fun create(navigationOptions: NavigationOptions): MapboxNavigation {
        LegacyMapboxNavigationInstanceHolder.peek()?.onDestroy()
        return MapboxNavigation(navigationOptions)
    }

    /**
     * Retrieve MapboxNavigation instance. Should be called after [create].
     *
     * @see [isCreated]
     */
    @JvmStatic
    @Deprecated(
        message = "Get the MapboxNavigation instance through MapboxNavigationObserver or" +
            " MapboxNavigationApp.current"
    )
    fun retrieve(): MapboxNavigation {
        if (!isCreated()) {
            throw RuntimeException("Need to create MapboxNavigation before using it.")
        }

        return LegacyMapboxNavigationInstanceHolder.peek()!!
    }

    /**
     * Destroy MapboxNavigation when your process/activity exits.
     */
    @JvmStatic
    @Deprecated(
        message = "MapboxNavigationApp will determine when to destroy MapboxNavigation instances"
    )
    fun destroy() {
        LegacyMapboxNavigationInstanceHolder.peek()?.onDestroy()
    }

    /**
     * Check if MapboxNavigation is created.
     */
    @JvmStatic
    fun isCreated(): Boolean {
        return LegacyMapboxNavigationInstanceHolder.peek()?.isDestroyed == false
    }
}
