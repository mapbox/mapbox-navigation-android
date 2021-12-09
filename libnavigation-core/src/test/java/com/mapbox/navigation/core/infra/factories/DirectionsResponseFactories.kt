package com.mapbox.navigation.core.infra.factories

import com.mapbox.api.directions.v5.models.DirectionsRoute

fun createDirectionsRoute(): DirectionsRoute {
    return DirectionsRoute.builder()
        .distance(5.0)
        .duration(9.0)
        .build()
}
