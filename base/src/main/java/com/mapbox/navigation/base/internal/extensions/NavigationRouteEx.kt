package com.mapbox.navigation.base.internal.extensions

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Find and return the destination coordinate [Point] used to request this [NavigationRoute].
 */
fun NavigationRoute.getDestination(): Point? = routeOptions.coordinatesList().lastOrNull()
