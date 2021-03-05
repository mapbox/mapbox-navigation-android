package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * The nearest route to a touch point on the [Map].
 *
 * @param route the route found
 */
class ClosestRouteValue internal constructor(val route: DirectionsRoute)
