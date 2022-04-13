package com.mapbox.navigation.dropin.component.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

/**
 * Defines the state for setting destination
 * @property point
 * @property features list of features containing POI details
 */
data class Destination internal constructor(
    val point: Point,
    val features: List<CarmenFeature>? = null
)
