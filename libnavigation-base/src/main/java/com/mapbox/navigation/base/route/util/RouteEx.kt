package com.mapbox.navigation.base.route.util

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Assume new [RouteOptionsNavigation] based on Route and progress of this one.
 */
// TODO need logic here
fun Route.redesignRouteOptions(routeProgress: RouteProgress): RouteOptionsNavigation? = this.routeOptions
