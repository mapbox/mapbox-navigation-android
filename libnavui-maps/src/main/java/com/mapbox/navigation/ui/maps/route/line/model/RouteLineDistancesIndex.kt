package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.Point

/**
 * @param point the upcoming, not yet visited point on the route
 * @param distanceRemaining distance remaining from the upcoming point
 */
data class RouteLineDistancesIndex(val point: Point, val distanceRemaining: Double)
