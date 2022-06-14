package com.mapbox.navigation.ui.app.internal.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

/**
 * Defines the state for setting destination
 * @property point
 * @property features list of features containing POI details
 */
data class Destination(
    val point: Point,
    val features: List<CarmenFeature>? = null
)
