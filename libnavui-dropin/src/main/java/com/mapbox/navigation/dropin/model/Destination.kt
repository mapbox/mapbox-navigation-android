package com.mapbox.navigation.dropin.model

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

data class Destination(
    val point: Point,
    val features: List<CarmenFeature>? = null
)
