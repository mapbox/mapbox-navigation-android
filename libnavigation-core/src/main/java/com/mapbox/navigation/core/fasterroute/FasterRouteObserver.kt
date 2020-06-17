package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.util.concurrent.TimeUnit

/**
 * Interface definition for an observer that is notified whenever the Navigation SDK checks for a faster route to the destination.
 */
interface FasterRouteObserver {

    /**
     * Overridable value to change when the next faster route will be checked.
     */
    fun restartAfterMillis(): Long = DEFAULT_INTERVAL_MILLIS

    /**
     * Invoked whenever a faster route was inspected.
     *
     * @param currentRoute the current route.
     * @param alternatives the routes where alternatives[0] may be faster
     * @param isAlternativeFaster true if the alternatives[0] is faster, false otherwise
     */
    fun onFasterRoute(
        currentRoute: DirectionsRoute,
        alternatives: List<DirectionsRoute>,
        isAlternativeFaster: Boolean
    )

    companion object {
        /**
         * The default and recommended interval for checking for faster routes.
         */
        val DEFAULT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5)
    }
}
