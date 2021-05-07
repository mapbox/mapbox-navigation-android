package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Updater is used for *Reroute* and *Route Alternatives* flow.
 *
 * It's used when turn-by-turn navigation goes off-route (see [OffRouteObserver])
 * and when route alternatives (see [RouteAlternativesController]) are requested.
 * For example, this is needed in order to filter the waypoints that have been completed.
 *
 * @see MapboxRouteOptionsUpdater
 */
fun interface RouteOptionsUpdater {

    /**
     * Provides a new [RouteOptions] instance based on the original request options and the current route progress.
     *
     * Returns *null* if a new [RouteOptions] instance cannot be combined based on the input given. When *null*
     * is returned new route is not fetched.
     */
    fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        location: Location?
    ): RouteOptionsResult

    /**
     * Describes a result of generating new options from the original request and the current route progress.
     */
    sealed class RouteOptionsResult {
        /**
         * Successful operation.
         *
         * @param routeOptions the recreated route option from the current route progress
         */
        data class Success(val routeOptions: RouteOptions) : RouteOptionsResult()

        /**
         * Failed operation.
         *
         * @param error reason
         */
        data class Error(val error: Throwable) : RouteOptionsResult()
    }
}
