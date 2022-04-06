package com.mapbox.navigation.dropin.component.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point

data class Destination internal constructor(
    val point: Point,
    val features: List<CarmenFeature>? = null
)
