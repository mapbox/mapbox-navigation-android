package com.mapbox.navigation.examples.manifesta.model.entity

import com.mapbox.geojson.Point

data class ManifestaLocation(
    val id: String,
    val name: String = "",
    val address: String = "",
    val notes: String = "",
    val position: Point,
    val geoHash: String = "",
    val tags: List<String> = emptyList()
)
