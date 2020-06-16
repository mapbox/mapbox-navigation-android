package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.fasterroute.FasterRouteController
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Provider is used for *Reroute* and *Faster Route* flow.
 *
 * It's used every time when turn-by-turn navigation goes off-route (see [OffRouteObserver])
 * and when needs to find faster route (see [FasterRouteController]).
 */
internal interface RouteOptionsProvider {

    /**
     * Provides a new *RouteOptions* instance based on *RouteOptions*, *RouteProgress*, and
     * *Location*
     *
     * Returns *null* if a new [RouteOptions] instance cannot be combined based on the input given. When *null*
     * is returned new route is not fetched
     */
    fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        location: Location?
    ): RouteOptionsResult

    sealed class RouteOptionsResult {
        data class Success(val routeOptions: RouteOptions) : RouteOptionsResult()
        data class Error(val error: Throwable) : RouteOptionsResult()
    }
}
