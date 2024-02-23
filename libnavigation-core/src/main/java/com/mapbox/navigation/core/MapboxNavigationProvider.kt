package com.mapbox.navigation.core

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.telemetry.TelemetryWrapper
import com.mapbox.navigation.utils.internal.ThreadController

/**
 * Singleton responsible for ensuring there is only one MapboxNavigation instance.
 */
@UiThread
@Deprecated(
    message = "Use MapboxNavigationApp to attach MapboxNavigation to lifecycles."
)
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
    @Deprecated(
        message = "Set the navigation options with MapboxNavigationApp.setup"
    )
    fun create(navigationOptions: NavigationOptions): MapboxNavigation {
        mapboxNavigation?.onDestroy()
        mapboxNavigation = MapboxNavigation(
            navigationOptions
        )

        return mapboxNavigation!!
    }

    @VisibleForTesting
    internal fun create(
        navigationOptions: NavigationOptions,
        threadController: ThreadController = ThreadController(),
        telemetryWrapper: TelemetryWrapper = TelemetryWrapper()
    ): MapboxNavigation {
        mapboxNavigation?.onDestroy()
        mapboxNavigation = MapboxNavigation(
            navigationOptions,
            threadController,
            telemetryWrapper,
        )

        return mapboxNavigation!!
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

        return mapboxNavigation!!
    }

    /**
     * Destroy MapboxNavigation when your process/activity exits.
     */
    @JvmStatic
    @Deprecated(
        message = "MapboxNavigationApp will determine when to destroy MapboxNavigation instances"
    )
    fun destroy() {
        mapboxNavigation?.onDestroy()
        mapboxNavigation = null
    }

    /**
     * Check if MapboxNavigation is created.
     */
    @JvmStatic
    fun isCreated(): Boolean {
        return mapboxNavigation?.isDestroyed == false
    }
}
