package com.mapbox.navigation.examples.manifesta.model.domain

import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaLocation

data class LocationCollection(
    val id: String,
    val name: String,
    val locations: List<ManifestaLocation>
)
