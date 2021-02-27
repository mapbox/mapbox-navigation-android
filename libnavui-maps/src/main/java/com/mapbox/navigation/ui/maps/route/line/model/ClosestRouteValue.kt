package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Represents the index of a route found by searching for the nearest route to to a map
 * click point. The index corresponds to the MapboxRouteArrowApi's collection of routes.
 *
 * @param routeIndex the index of the route in the collection
 */
class ClosestRouteValue internal constructor(val routeIndex: Int)
