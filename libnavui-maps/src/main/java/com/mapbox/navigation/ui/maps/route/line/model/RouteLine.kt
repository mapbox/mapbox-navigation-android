package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Represents a route and an optional identification of used for representing routes on the map.
 *
 * @param route a directions route
 * @param identifier an optional identifier for the directions route which can be used to
 * influence color of the route when it is an alternative route.
 */
data class RouteLine(val route: DirectionsRoute, val identifier: String?)
