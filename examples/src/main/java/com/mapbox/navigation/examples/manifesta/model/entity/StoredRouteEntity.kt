package com.mapbox.navigation.examples.manifesta.model.entity

import java.util.UUID

data class StoredRouteEntity(
    val id: String = UUID.randomUUID().toString().replace("-", ""),
    val alias: String,
    val routeAsJson: String
)

