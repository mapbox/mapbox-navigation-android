package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Contains the various progress states that can occur while navigating.
 */
enum class RouteProgressState {

    /**
     * The [com.mapbox.api.directions.v5.models.DirectionsRoute] provided
     * via [com.mapbox.navigation.core.MapboxNavigation.setRoutes]
     * when in `Active Guidance` is not valid.
     */
    ROUTE_INVALID,

    /**
     * The [DirectionsRoute] is valid
     * and [com.mapbox.navigation.core.MapboxNavigation] is waiting for
     * sufficient [android.location.Location] updates
     * from the [com.mapbox.android.core.location.LocationEngine].
     */
    ROUTE_INITIALIZED,

    /**
     * The user has arrived at the destination of the given [com.mapbox.api.directions.v5.models.RouteLeg].
     */
    ROUTE_COMPLETE,

    /**
     * [com.mapbox.navigation.core.MapboxNavigation] is now confidently tracking the
     * location updates and processing them against the route.
     */
    LOCATION_TRACKING,

    /**
     * A lack of [android.location.Location] updates has caused a lack of confidence in the
     * progress updates being sent.
     */
    LOCATION_STALE,

    /**
     * State when we detect an off-route.
     */
    OFF_ROUTE,

    /**
     * State when we start following a route.
     *
     * After a certain number of tracking points, we gain confidence and switch to tracking state.
     * We do map-matching rather than route line snapping during this state.
     */
    ROUTE_UNCERTAIN
}
