package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute

data class RouteAlternative internal constructor(
    val directionsRoute: DirectionsRoute,
    val currentRouteFork: RouteIntersection,
    val alternativeRouteFork: RouteIntersection
)
