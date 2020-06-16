package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * A wrapper class for [DirectionsRoute] that includes an identity property that can be used
 * to override the color of the route line.
 *
 * @param route a [DirectionsRoute] to associate with an identifier
 * @param routeIdentifier the identifier for the route
 */
data class IdentifiableRoute(val route: DirectionsRoute, val routeIdentifier: String)
