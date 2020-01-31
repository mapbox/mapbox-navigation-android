package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Contains the various progress states that can occur while navigating.
 */
enum class RouteProgressState {

    /**
     * The [com.mapbox.api.directions.v5.models.DirectionsRoute] provided
     * via [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation.startNavigation]
     * is not valid.
     */
    ROUTE_INVALID,

    /**
     * The [DirectionsRoute] is valid
     * and [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation] is waiting for
     * sufficient [android.location.Location] updates
     * from the [com.mapbox.android.core.location.LocationEngine].
     */
    ROUTE_INITIALIZED,

    /**
     * The user has arrived at the destination of the given [com.mapbox.api.directions.v5.models.RouteLeg].
     */
    ROUTE_ARRIVED,

    /**
     * [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation] is now confidently tracking the
     * location updates and processing them against the route.
     */
    LOCATION_TRACKING,

    /**
     * A lack of [android.location.Location] updates from the phone has caused lack of confidence in the
     * progress updates being sent.
     */
    LOCATION_STALE,

    ROUTE_UNCERTAIN
}
