package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.util.concurrent.TimeUnit

/**
 * Interface definition for an observer that get's notified whenever the SDK finds a faster route to the destination.
 */
interface FasterRouteObserver {

    /**
     * Overridable value to change when the next faster route will be checked.
     */
    fun restartAfterMillis(): Long = DEFAULT_INTERVAL_MILLIS

    /**
     * Invoked whenever a faster route was inspected.
     *
     * @param currentRoute reference to the current route.
     * @param alternativeRoute reference to the alternative checked.
     * @param isAlternativeFaster true if the alternativeRoute is faster, false otherwise
     */
    fun onFasterRoute(currentRoute: DirectionsRoute, alternativeRoute: DirectionsRoute, isAlternativeFaster: Boolean)

    companion object {
        /**
         * The default and recommended interval for checking for faster routes.
         */
        val DEFAULT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5)
    }
}
