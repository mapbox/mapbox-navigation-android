package com.mapbox.navigation.ui.camera

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * This class holds all information related to a route and a user's progress along the route. It
 * also provides useful information (screen configuration and target distance) which can be used to
 * make additional configuration changes to the map's camera.
 *
 */
data class RouteInformation(
    /**
     * The current route the user is navigating along. This value will update when reroutes occur
     * and it will be null if the [RouteInformation] is generated from an update to route
     * progress or from an orientation change.
     * @return current route
     */
    val route: DirectionsRoute?,
    /**
     * The user's current location along the route. This value will update when orientation changes
     * occur as well as when progress along a route changes.
     * @return current location
     */
    val location: Location?,
    /**
     * The user's current progress along the route.
     * @return current progress along the route.
     */
    val routeProgress: RouteProgress?
)
